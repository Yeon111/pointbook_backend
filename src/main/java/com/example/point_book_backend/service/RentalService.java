package com.example.point_book_backend.service;

import com.example.point_book_backend.domain.Book;
import com.example.point_book_backend.domain.Rental;
import com.example.point_book_backend.domain.User;
import com.example.point_book_backend.repository.BookRepository;
import com.example.point_book_backend.repository.RentalRepository;
import com.example.point_book_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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

        // 대여 기록 생성 및 인증번호 세팅 👈
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
            // 예시로 보관함 번호를 숫자로 추출하거나 기본값 부여 (필요시 조정 가능)
            rental.setCabinetSlot(1);
        }

        int rentalFee = (totalPoints != null) ? totalPoints : 100;
        if (user.getPointBalance() < rentalFee) {
            throw new IllegalStateException("포인트가 부족하여 대여할 수 없습니다.");
        }
        user.setPointBalance(user.getPointBalance() - rentalFee);

        if (user.getPointBalance() <= 0) {
            user.setPenaltyStatus(true);
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
    }

    @Transactional
    public void saveSequentialAuthCodes(Integer cabinetSlot, String auth_code_1, String auth_code_2) {
        Rental rental = rentalRepository.findByCabinetSlot(cabinetSlot)
                .orElseGet(() -> {
                    Rental latestRental = rentalRepository.findTopByOrderByIdDesc()
                            .orElseThrow(() -> new IllegalArgumentException("활성화된 대여 내역이 없습니다."));
                    latestRental.setCabinetSlot(cabinetSlot);
                    return latestRental;
                });

        rental.setAuth_code_1(auth_code_1);
        rental.setAuth_code_2(auth_code_2);
    }

    // 🚨 [추가/수정] 1단계 인증번호만 정확히 비교
    public boolean verifyStep1Code(Integer cabinetSlot, String inputCode) {
        Rental rental = rentalRepository.findByCabinetSlot(cabinetSlot)
                .orElseThrow(() -> new IllegalArgumentException("해당 수납장의 대여 내역을 찾을 수 없습니다."));

        return rental.getAuth_code_1() != null && rental.getAuth_code_1().equals(inputCode);
    }

    // 🚨 [추가/수정] 2단계 인증번호만 정확히 비교
    public boolean verifyStep2Code(Integer cabinetSlot, String inputCode) {
        Rental rental = rentalRepository.findByCabinetSlot(cabinetSlot)
                .orElseThrow(() -> new IllegalArgumentException("해당 수납장의 대여 내역을 찾을 수 없습니다."));

        return rental.getAuth_code_2() != null && rental.getAuth_code_2().equals(inputCode);
    }
}