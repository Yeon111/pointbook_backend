package com.example.point_book_backend.service;

import com.example.point_book_backend.domain.Book;
import com.example.point_book_backend.domain.Coupon;
import com.example.point_book_backend.domain.Rental;
import com.example.point_book_backend.domain.User;
import com.example.point_book_backend.repository.BookRepository;
import com.example.point_book_backend.repository.RentalRepository;
import com.example.point_book_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RentalService {

    @Autowired private RentalRepository rentalRepository;
    @Autowired private BookRepository bookRepository;
    @Autowired private UserRepository userRepository;

    @Transactional
    public Rental rentBook(String userName, Long bookId, String paymentMethod, Integer rentalHours, Integer totalPoints, String auth_code_1) {

        User user = userRepository.findByName(userName)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다: " + userName));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 도서 ID입니다: " + bookId));

        if (book.getIsAvailable() != null && !book.getIsAvailable()) {
            throw new IllegalStateException("이미 누군가 대여 중인 도서입니다.");
        }

        // 마감 시간(dueTime) 계산
        LocalDateTime dueTime = LocalDateTime.now().plusHours(rentalHours != null ? rentalHours : 24);

        // 도서 상태 변경
        book.setIsAvailable(false);
        book.setBorrower(userName);
        book.setRentalTimeLeft(rentalHours != null ? rentalHours.longValue() : 24L);
        book.setRentalDueDate(dueTime);
        bookRepository.saveAndFlush(book);

        // 대여 기록 생성 및 인증번호 세팅 (1단계만 저장, step1Passed 초기값 false)
        Rental rental = new Rental();
        rental.setUserId(user.getId());
        rental.setBookId(bookId);
        rental.setRentalType(paymentMethod);
        rental.setStartTime(LocalDateTime.now());
        rental.setDueTime(dueTime);
        rental.setStatus("대여중");
        rental.setAuth_code_1(auth_code_1);
        rental.setStep1Passed(false);

        // 책에 지정된 보관함 번호가 있다면 rental의 cabinetSlot에도 자동 반영
        if (book.getStorageNo() != null) {
            rental.setCabinetSlot(1);
        }

        // 결제 수단 확인 (쿠폰 vs 포인트)
        if ("쿠폰".equals(paymentMethod)) {
            if (user.getRentalCoupons() == null || user.getRentalCoupons().isEmpty()) {
                throw new IllegalStateException("사용 가능한 무료 대여 쿠폰이 없습니다.");
            }
            user.getRentalCoupons().remove(0);
        } else {
            int rentalFee = (totalPoints != null) ? totalPoints : 100;
            if (user.getPointBalance() < rentalFee) {
                throw new IllegalStateException("포인트가 부족하여 대여할 수 없습니다.");
            }
            user.setPointBalance(user.getPointBalance() - rentalFee);

            if (user.getPointBalance() <= 0) {
                user.setPenaltyStatus(true);
            }
        }

        // 스탬프 적립 및 10개 달성 시 쿠폰 발급
        user.setStamps(user.getStamps() + 1);

        if (user.getStamps() >= 10) {
            user.setStamps(0);
            Coupon newCoupon = new Coupon(
                    "스탬프 10개 달성 무료 도서 대여 쿠폰",
                    java.time.LocalDate.now().toString(),
                    user
            );
            user.getRentalCoupons().add(newCoupon);
        }

        userRepository.saveAndFlush(user);
        return rentalRepository.saveAndFlush(rental);
    }

    @Transactional
    public void returnBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("도서 없음"));

        book.setIsAvailable(true);
        book.setBorrower("");
        book.setRentalTimeLeft(0L);
        book.setRentalDueDate(null);
        bookRepository.saveAndFlush(book);

        List<Rental> rentals = rentalRepository.findAll();
        for (Rental r : rentals) {
            if (r.getBookId().equals(bookId) && "대여중".equals(r.getStatus())) {
                r.setStatus("반납/회수완료");
                r.setReturnTime(LocalDateTime.now());
                rentalRepository.saveAndFlush(r);
                break;
            }
        }
    }

    // 💡 1단계 인증 번호만 저장하도록 수정 (2단계 제거)
    @Transactional
    public void saveSequentialAuthCodes(Integer cabinetSlot, String auth_code_1) {
        Rental rental = rentalRepository.findTopByOrderByIdDesc()
                .orElseThrow(() -> new IllegalArgumentException("활성화된 대여 내역이 없습니다."));
        rental.setCabinetSlot(cabinetSlot);
        rental.setAuth_code_1(auth_code_1);
    }

    // ==========================================
    // 💡 [신규] 대여/반납 전용 1단계 검증 (Rental 테이블만 검사 + step1Passed = true 기록)
    // ==========================================
    @Transactional
    public boolean verifyRentalStep1(Integer cabinetSlot, String inputCode) {
        String cleanInput = inputCode != null ? inputCode.trim() : "";
        boolean isMatch = false;

        List<Rental> rentals = rentalRepository.findAll();
        for (Rental r : rentals) {
            if (r.getCabinetSlot() != null && r.getCabinetSlot().equals(cabinetSlot)
                    && "대여중".equals(r.getStatus())
                    && cleanInput.equals(r.getAuth_code_1())) {

                r.setStep1Passed(true); // 통과 상태 기록
                rentalRepository.saveAndFlush(r);
                isMatch = true;
                break;
            }
        }

        System.out.println("🚨 [대여 1단계 검증] 수납장: " + cabinetSlot + ", 입력번호: [" + cleanInput + "] -> 결과: " + isMatch);
        return isMatch;
    }

    // ==========================================
    // 💡 [신규] 보관/회수 전용 1단계 검증 (Book 테이블만 검사 + step1Passed = true 기록)
    // ==========================================
    @Transactional
    public boolean verifyStorageStep1(Integer cabinetSlot, String inputCode) {
        String cleanInput = inputCode != null ? inputCode.trim() : "";
        boolean isMatch = false;

        List<Book> books = bookRepository.findAll();
        for (Book book : books) {
            if (book.getAuthCode1() != null && book.getAuthCode1().equals(cleanInput)) {
                book.setStep1Passed(true); // 통과 상태 기록
                bookRepository.saveAndFlush(book);
                isMatch = true;
                break;
            }
        }

        System.out.println("🚨 [보관 1단계 검증] 수납장: " + cabinetSlot + ", 입력번호: [" + cleanInput + "] -> 결과: " + isMatch);
        return isMatch;
    }

    // ==========================================
    // 💡 [신규] 앱 전용: 해당 수납장이 1단계 통과 상태인지 확인
    // ==========================================
    public boolean appCheckStep1Status(Integer cabinetSlot) {
        boolean isPassed = false;

        // 1. Rental 테이블 확인
        List<Rental> rentals = rentalRepository.findAll();
        for (Rental r : rentals) {
            if (r.getCabinetSlot() != null && r.getCabinetSlot().equals(cabinetSlot)
                    && Boolean.TRUE.equals(r.getStep1Passed())) {
                isPassed = true;
                break;
            }
        }

        // 2. Rental에 없으면 Book 테이블 확인
        if (!isPassed) {
            List<Book> books = bookRepository.findAll();
            for (Book b : books) {
                if (Boolean.TRUE.equals(b.getStep1Passed())) {
                    isPassed = true;
                    break;
                }
            }
        }

        return isPassed;
    }
}