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

    @PostMapping("/books")
    public ResponseEntity<?> createBook(
            @RequestParam("title") String title,
            @RequestParam("author") String author,
            @RequestParam(value = "publisher", required = false) String publisher,
            @RequestParam(value = "condition", required = false) String condition,
            @RequestParam(value = "storageNo", required = false) String storageNo,
            @RequestParam(value = "lockerNo", required = false) String lockerNo,
            @RequestParam(value = "owner", required = false) String owner,
            @RequestParam(value = "ownerPin", required = false) String ownerPin,
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
            book.setLockerNo(lockerNo);
            book.setOwner(owner);
            book.setOwnerPin(ownerPin);

            book.setIsAvailable(false);
            book.setBorrower("");

            book.setAuthCode1(authCode1);
            book.setStep1Passed(false);

            String jsonImageArray = imageUrls.isEmpty() ? null : "[\"" + String.join("\",\"", imageUrls) + "\"]";
            book.setImage(jsonImageArray);

            bookRepository.save(book);

            return ResponseEntity.ok(Map.of("success", true, "message", "도서 및 이미지가 성공적으로 등록되었습니다."));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // =========================================================================
    // 🚀 [추가됨] 회수하기(Retrieve) 시 하드웨어 검증을 위해 새 임시 인증번호 업데이트 API
    // =========================================================================
    @PostMapping("/books/auth-code")
    public ResponseEntity<?> updateBookAuthCode(@RequestBody Map<String, Object> request) {
        try {
            Long bookId = Long.valueOf(request.get("bookId").toString());
            String newAuthCode = request.get("auth_code_1").toString();

            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 도서입니다."));

            // 기존 인증번호를 앱에서 새로 생성한 회수용 번호로 덮어씌움
            book.setAuthCode1(newAuthCode);
            book.setStep1Passed(false); // 새로운 인증을 위해 통과 상태 초기화

            bookRepository.saveAndFlush(book);

            return ResponseEntity.ok(Map.of("success", true, "message", "회수용 인증번호 동기화 완료"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // =========================================================================
    // 🚀 [수정됨] 앱에서 요청하는 보관/회수 검증 (앱도 success, open 모두 수신)
    // =========================================================================
    @PostMapping("/app/verify-storage-step1")
    public ResponseEntity<?> appVerifyStorageStep1(@RequestBody Map<String, Object> request) {
        Map<String, Object> res = new HashMap<>();
        try {
            String slotStr = request.get("cabinetSlot") != null ? request.get("cabinetSlot").toString() : "1";
            List<Book> books = bookRepository.findAllByOrderByIdDesc();
            boolean isPassed = false;

            for (Book book : books) {
                if (book.getStorageNo() != null && book.getStorageNo().contains(slotStr)) {
                    if (book.getStep1Passed() != null && book.getStep1Passed()) {
                        isPassed = true;
                    }
                    break;
                }
            }
            res.put("success", isPassed);
            res.put("open", isPassed); // 앱, 하드웨어 호환성 확보
            return ResponseEntity.ok(res);

        } catch (Exception e) {
            e.printStackTrace();
            res.put("success", false);
            res.put("open", false);
            return ResponseEntity.ok(res);
        }
    }

    // =========================================================================
    // 🚨 [핵심 수정됨] 하드웨어(ESP8266)에서 요청하는 보관/회수 검증
    // 띄어쓰기 문제로 하드웨어가 인식 못하는 현상을 원천 차단하기 위해 문자열로 쏴줌!
    // =========================================================================
    @PostMapping("/hardware/verify-storage-step1")
    public ResponseEntity<?> hardwareVerifyStorage(@RequestBody Map<String, Object> request) {
        try {
            Integer cabinetSlot = request.get("cabinetSlot") != null ? Integer.valueOf(request.get("cabinetSlot").toString()) : 1;
            String inputCode = request.get("authCode") != null ? request.get("authCode").toString() :
                    (request.get("auth_code_1") != null ? request.get("auth_code_1").toString() : "");

            boolean isMatch = rentalService.verifyStorageStep1(cabinetSlot, inputCode);

            // 아두이노가 찾는 정확한 포맷("open":true)으로 공백 없이 문자열 자체를 리턴합니다.
            String safeJsonResponse = isMatch ? "{\"success\":true,\"open\":true}" : "{\"success\":false,\"open\":false}";

            return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .body(safeJsonResponse);
        } catch (Exception e) {
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .body("{\"success\":false,\"open\":false}");
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