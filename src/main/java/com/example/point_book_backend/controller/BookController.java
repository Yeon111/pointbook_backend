package com.example.point_book_backend.controller;

import com.example.point_book_backend.domain.Book;
import com.example.point_book_backend.domain.Report;
import com.example.point_book_backend.domain.User;
import com.example.point_book_backend.repository.BookRepository;
import com.example.point_book_backend.repository.UserRepository;
import com.example.point_book_backend.service.RentalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.ArrayList;
import java.util.UUID;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class BookController {

    @Autowired private BookRepository bookRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RentalService rentalService;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody Map<String, String> request) {
        String studentId = request.get("studentId");
        String name = request.get("name");
        String pin = request.get("pin");

        if (userRepository.findByStudentId(studentId).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "이미 가입된 학번입니다."));
        }

        User user = new User();
        user.setStudentId(studentId);
        user.setName(name);
        user.setPin(pin);

        // 🚨 [추가된 부분] 프론트엔드 요청: 가입 시 추천 코드(8자리) 무조건 생성 및 저장
        String randomCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        user.setReferralCode(randomCode);

        userRepository.save(user);

        return ResponseEntity.ok(Map.of("success", true, "message", "회원가입 완료"));
    }

    // 💡 참고: 로그인 API는 이미 UserController에 구현되어 있으므로, 충돌 방지를 위해 여기서 완전히 제거했습니다!

    @GetMapping("/books")
    public List<Book> getBooks() {
        return bookRepository.findAllByOrderByIdDesc();
    }

    // ==========================================
    // 🚨 [최종 수정] 도서 등록 시 다중 사진 업로드(FormData) 완벽 대응
    // ==========================================
    @PostMapping("/books")
    public ResponseEntity<?> createBook(
            @RequestParam("title") String title,
            @RequestParam("author") String author,
            @RequestParam(value = "publisher", required = false) String publisher,
            @RequestParam(value = "condition", required = false) String condition,
            @RequestParam(value = "storageNo", required = false) String storageNo,
            @RequestParam(value = "owner", required = false) String owner,
            @RequestParam(value = "images", required = false) List<MultipartFile> images) {

        try {
            List<String> imageUrls = new ArrayList<>();

            // 1. 프론트에서 실제 파일(사진)이 넘어왔다면 EC2 서버 폴더에 물리적으로 저장
            if (images != null && !images.isEmpty()) {
                for (MultipartFile file : images) {
                    if (file.isEmpty()) continue;

                    // 파일 이름이 겹치지 않게 고유 식별자(UUID) 생성
                    String uuidFilename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

                    // AWS EC2 서버의 /home/ubuntu/uploads/ 폴더에 저장
                    File dest = new File("/home/ubuntu/uploads/" + uuidFilename);
                    file.transferTo(dest);

                    // 누구나 볼 수 있는 공용 URL 생성 후 리스트에 담기
                    String publicUrl = "/uploads/" + uuidFilename;
                    imageUrls.add(publicUrl);
                }
            }

            // 2. 도서 엔티티 생성 및 정보 세팅
            Book book = new Book();
            book.setTitle(title);
            book.setAuthor(author);
            book.setPublisher(publisher);
            book.setCondition(condition);
            book.setStorageNo(storageNo);
            book.setOwner(owner);

            // 기본 상태 세팅
            book.setIsAvailable(true);
            book.setBorrower("");

            // 3. 다중 이미지 URL을 JSON 배열 형태의 문자열로 변환하여 저장
            String jsonImageArray = imageUrls.isEmpty() ? null : "[\"" + String.join("\",\"", imageUrls) + "\"]";
            book.setImage(jsonImageArray);

            bookRepository.save(book);

            return ResponseEntity.ok(Map.of("success", true, "message", "도서 및 이미지가 성공적으로 등록되었습니다."));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ==========================================
    // 🚨 프론트엔드 다중 사진 업로드(FormData) 완벽 대응 (신고 기능)
    // ==========================================
    @PostMapping("/reports")
    public ResponseEntity<?> addReport(
            @RequestParam("bookId") Long bookId,
            @RequestParam("reporter") String reporter,
            @RequestParam("detail") String detail,
            @RequestParam("date") String date,
            @RequestParam(value = "images", required = false) List<MultipartFile> images) {

        try {
            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new IllegalArgumentException("도서를 찾을 수 없습니다."));

            List<String> imageUrls = new ArrayList<>();

            // 1. 프론트에서 실제 파일이 넘어왔다면 EC2 서버 폴더에 물리적으로 저장
            if (images != null && !images.isEmpty()) {
                for (MultipartFile file : images) {
                    if (file.isEmpty()) continue;

                    // 파일 이름이 겹치지 않게 고유 식별자(UUID) 생성
                    String uuidFilename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

                    // AWS EC2 서버의 /home/ubuntu/uploads/ 폴더에 저장
                    File dest = new File("/home/ubuntu/uploads/" + uuidFilename);
                    file.transferTo(dest);

                    // 누구나 볼 수 있는 공용 URL 생성 후 리스트에 담기
                    String publicUrl = "/uploads/" + uuidFilename;
                    imageUrls.add(publicUrl);
                }
            }

            Report report = new Report();
            report.setContent(detail != null ? detail : "내용 없음");
            report.setReporter(reporter != null ? reporter : "익명");

            // 프론트엔드의 JSON.parse() 에러를 방지하기 위해 완벽한 문자열 배열 ["url1", "url2"] 형태로 가공하여 저장
            String jsonImageArray = imageUrls.isEmpty() ? null : "[\"" + String.join("\",\"", imageUrls) + "\"]";
            report.setImage(jsonImageArray);

            report.setReportTime(java.time.LocalDateTime.now());

            report.setBook(book);
            book.getReports().add(report);
            bookRepository.saveAndFlush(book);

            return ResponseEntity.ok(Map.of("success", true, "message", "신고가 DB에 정상적으로 접수되었습니다."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/reports/{reportId}")
    public ResponseEntity<?> deleteReport(@PathVariable Long reportId) {
        try {
            List<Book> allBooks = bookRepository.findAll();
            boolean removed = false;

            for (Book book : allBooks) {
                removed = book.getReports().removeIf(report -> report.getId().equals(reportId));
                if (removed) {
                    bookRepository.saveAndFlush(book);
                    break;
                }
            }

            if (removed) {
                return ResponseEntity.ok(Map.of("success", true, "message", "DB에서 해당 신고 내역이 영구 삭제되었습니다."));
            } else {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "해당 ID의 신고를 찾을 수 없습니다."));
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/users/points")
    public ResponseEntity<?> updatePoints(@RequestBody Map<String, Object> request) {
        try {
            String studentId = request.get("studentId").toString();
            User user = userRepository.findByStudentId(studentId)
                    .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

            int pointData = Integer.parseInt(request.get("points").toString());

            user.setPointBalance(pointData);
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "포인트가 DB에 성공적으로 업데이트되었습니다.",
                    "currentPoints", user.getPointBalance()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/books/{id}")
    public ResponseEntity<?> deleteBook(@PathVariable Long id) {
        try {
            Book book = bookRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("도서 없음"));
            if (book.getIsAvailable() != null && !book.getIsAvailable()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "대여 중인 도서는 회수 불가"));
            }
            bookRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "도서 영구 삭제 완료"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "삭제 실패"));
        }
    }
}