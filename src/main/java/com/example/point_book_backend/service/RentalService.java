package com.example.point_book_backend.service;

import com.example.point_book_backend.domain.Book;
import com.example.point_book_backend.domain.Coupon; // 🚨 [추가] 쿠폰 엔티티 임포트
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
    public Rental rentBook(String userName, Long bookId, String paymentMethod, Integer rentalHours, Integer totalPoints, String auth_code_1, String auth_code_2) {

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

        // 대여 기록 생성 및 인증번호 세팅
        Rental rental = new Rental();
        rental.setUserId(user.getId());
        rental.setBookId(bookId);
        rental.setRentalType(paymentMethod);
        rental.setStartTime(LocalDateTime.now());
        rental.setDueTime(dueTime);
        rental.setStatus("대여중");

        // 프론트엔드가 보낸 1단계, 2단계 인증번호와 수납장 번호 매핑
        rental.setAuth_code_1(auth_code_1);
        rental.setAuth_code_2(auth_code_2);

        // 책에 지정된 보관함 번호가 있다면 rental의 cabinetSlot에도 자동 반영
        if (book.getStorageNo() != null) {
            rental.setCabinetSlot(1);
        }

        // 👇👇👇 [핵심 수정 부분] 결제 수단 확인 (쿠폰 vs 포인트) 👇👇👇
        if ("쿠폰".equals(paymentMethod)) {
            // 쿠폰 결제일 경우
            if (user.getRentalCoupons() == null || user.getRentalCoupons().isEmpty()) {
                throw new IllegalStateException("사용 가능한 무료 대여 쿠폰이 없습니다.");
            }
            // 보유 중인 쿠폰 중 제일 오래된 것(0번째 인덱스) 1장 차감
            user.getRentalCoupons().remove(0);
        } else {
            // 기존 로직: 포인트 결제일 경우
            int rentalFee = (totalPoints != null) ? totalPoints : 100;
            if (user.getPointBalance() < rentalFee) {
                throw new IllegalStateException("포인트가 부족하여 대여할 수 없습니다.");
            }
            user.setPointBalance(user.getPointBalance() - rentalFee);

            if (user.getPointBalance() <= 0) {
                user.setPenaltyStatus(true);
            }
        }

        // 👇👇👇 [신규 추가 부분] 스탬프 적립 및 10개 달성 시 쿠폰 발급 👇👇👇
        user.setStamps(user.getStamps() + 1); // 대여 1건당 스탬프 +1

        if (user.getStamps() >= 10) {
            user.setStamps(0); // 10개를 채웠으므로 다시 0으로 초기화

            // 무료 대여 쿠폰 1장 새로 생성
            Coupon newCoupon = new Coupon(
                    "스탬프 10개 달성 무료 도서 대여 쿠폰",
                    java.time.LocalDate.now().toString(),
                    user
            );
            // 유저의 쿠폰함에 추가
            user.getRentalCoupons().add(newCoupon);
        }
        // 👆👆👆 수정 완료 👆👆👆

        // 변경된 유저 정보(포인트 차감/쿠폰 차감/스탬프/새 쿠폰 발급 내역) DB 저장
        userRepository.saveAndFlush(user);

        return rentalRepository.saveAndFlush(rental);
    }

    @Transactional
    public void returnBook(Long bookId) {
        // 1. 도서 상태를 다시 '대여 가능'으로 복구
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("도서 없음"));

        book.setIsAvailable(true);
        book.setBorrower("");
        book.setRentalTimeLeft(0L);
        book.setRentalDueDate(null);
        bookRepository.saveAndFlush(book);

        // 2. Rental(대여) 테이블에서 '대여중'인 기록을 찾아 '반납/회수완료' 처리 및 시간 기록
        List<Rental> rentals = rentalRepository.findAll();
        for (Rental r : rentals) {
            if (r.getBookId().equals(bookId) && "대여중".equals(r.getStatus())) {
                r.setStatus("반납/회수완료"); // 상태 변경
                r.setReturnTime(LocalDateTime.now()); // 현재 시간을 반납 시간으로 저장
                rentalRepository.saveAndFlush(r);
                break;
            }
        }
    }

    // Repository 변경에 맞춰 에러가 나지 않도록 최신 데이터 조회 방식으로 변경
    @Transactional
    public void saveSequentialAuthCodes(Integer cabinetSlot, String auth_code_1, String auth_code_2) {
        Rental rental = rentalRepository.findTopByOrderByIdDesc()
                .orElseThrow(() -> new IllegalArgumentException("활성화된 대여 내역이 없습니다."));
        rental.setCabinetSlot(cabinetSlot);
        rental.setAuth_code_1(auth_code_1);
        rental.setAuth_code_2(auth_code_2);
    }

    // 1단계 검증: 키패드 번호로 직접 DB 조회 (과거 데이터 무시)
    public boolean verifyStep1Code(Integer cabinetSlot, String inputCode) {
        String cleanInput = inputCode != null ? inputCode.trim() : "";

        // 하드웨어가 보낸 번호가 DB에 존재하는지 직접 족집게처럼 찾아냄
        boolean isMatch = rentalRepository.findValidRentalForStep1(cabinetSlot, cleanInput).isPresent();

        System.out.println("🚨 [1단계 인증] 수납장: " + cabinetSlot + ", 하드웨어가 쏜 번호: [" + cleanInput + "] -> 결과: " + isMatch);
        return isMatch;
    }

    // 2단계 검증: 키패드 번호로 직접 DB 조회 (과거 데이터 무시)
    public boolean verifyStep2Code(Integer cabinetSlot, String inputCode) {
        String cleanInput = inputCode != null ? inputCode.trim() : "";

        boolean isMatch = rentalRepository.findValidRentalForStep2(cabinetSlot, cleanInput).isPresent();

        System.out.println("🚨 [2단계 인증] 수납장: " + cabinetSlot + ", 하드웨어가 쏜 번호: [" + cleanInput + "] -> 결과: " + isMatch);
        return isMatch;
    }
}