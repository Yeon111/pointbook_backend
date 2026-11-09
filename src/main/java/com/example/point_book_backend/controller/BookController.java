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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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

        user.setReferralCode("USER_" + studentId);

        userRepository.save(user);

        return ResponseEntity.ok(Map.of("success", true, "message", "회원가입 완료"));
    }

    @GetMapping("/books")
    public List<Book> getBooks() {
        return bookRepository.findAllByOrderByIdDesc();
    }

    // ==========================================
    // 💡 [수정 완료] 도서 등록 시 1단계 인증 코드만 받도록 변경 (2단계 제거)
    // ==========================================
    @PostMapping("/books")
    public ResponseEntity<?> createBook(
            @RequestParam("title") String title,
            @RequestParam("author") String author,
            @RequestParam(value = "publisher", required = false) String publisher,
            @RequestParam(value = "condition", required = false) String condition,
            @RequestParam(value = "storageNo", required = false) String storageNo,
            @RequestParam(value = "owner", required = false) String owner,
            // 👇 1단계 인증코드만 파라미터로 받음 (2단계 제거)
            @RequestParam(value = "auth_code_1", required = false) String authCode1,
            @RequestParam(value = "images", required = false) List<MultipartFile> images) {

        try {
            List<String> imageUrls = new ArrayList<>();

            if (images != null && !images.isEmpty()) {
                for (MultipartFile file : images) {
                    if (file.isEmpty()) continue;

                    String uuidFilename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                    File dest = new File("/home/ubuntu/uploads/" + uuidFilename);
                    file.transferTo(dest);

                    String publicUrl = "/uploads/" + uuidFilename;
                    imageUrls.add(publicUrl);
                }
            }

            Book book = new Book();
            book.setTitle(title);
            book.setAuthor(author);
            book.setPublisher(publisher);
            book.setCondition(condition);
            book.setStorageNo(storageNo);
            book.setOwner(owner);

            book.setIsAvailable(true);
            book.setBorrower("");

            // 👇 1단계 인증 코드만 Book DB 필드에 저장
            book.setAuthCode1(authCode1);
            book.setStep1Passed(false); // 초기 상태 false 설정

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
    // 📱 [앱 전용] 보관/회수 하드웨어 인증 통과 여부 확인 API (새로 추가됨)
    // ==========================================
    @PostMapping("/app/verify-storage-step1")
    public ResponseEntity<?> appVerifyStorageStep1(@RequestBody Map<String, Object> request) {
        Map<String, Object> res = new HashMap<>();
        try {
            // 1. 앱에서 보낸 사물함 번호 받기 (Book 엔티티는 String 타입인 storageNo를 쓰므로 String으로 변환)
            String storageNo = request.get("cabinetSlot") != null ? request.get("cabinetSlot").toString() : "1";

            // 2. BookRepository를 뒤져서 해당 사물함의 가장 최근 도서를 가져옴
            Optional<Book> bookOpt = bookRepository.findTopByStorageNoOrderByIdDesc(storageNo);

            // 3. 책이 존재하고, 그 책의 step1Passed 값이 true인지 확인
            boolean isPassed = false;
            if (bookOpt.isPresent()) {
                Book book = bookOpt.get();
                if (book.getStep1Passed() != null && book.getStep1Passed()) {
                    isPassed = true;
                }
            }

            // 4. 앱(규빈이)에게 결과 전달
            res.put("success", isPassed);
            return ResponseEntity.ok(res);

        } catch (Exception e) {
            e.printStackTrace();
            res.put("success", false);
            return ResponseEntity.ok(res);
        }
    }

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

            if (images != null && !images.isEmpty()) {
                for (MultipartFile file : images) {
                    if (file.isEmpty()) continue;

                    String uuidFilename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                    File dest = new File("/home/ubuntu/uploads/" + uuidFilename);
                    file.transferTo(dest);

                    String publicUrl = "/uploads/" + uuidFilename;
                    imageUrls.add(publicUrl);
                }
            }

            Report report = new Report();
            report.setContent(detail != null ? detail : "내용 없음");
            report.setReporter(reporter != null ? reporter : "익명");

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