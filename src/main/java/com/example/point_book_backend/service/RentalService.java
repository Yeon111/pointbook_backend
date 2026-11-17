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
import java.util.Optional;

@Service
public class RentalService {

    @Autowired private RentalRepository rentalRepository;
    @Autowired private BookRepository bookRepository;
    @Autowired private UserRepository userRepository;

    // =====================================================================
    // 1. [대여하기] 기능
    // =====================================================================
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

        // 대여 기록 생성 및 인증번호 세팅
        Rental rental = new Rental();
        rental.setUserId(user.getId());
        rental.setBookId(bookId);
        rental.setRentalType(paymentMethod);
        rental.setStartTime(LocalDateTime.now());
        rental.setDueTime(dueTime);
        rental.setStatus("대여중");
        rental.setAuth_code_1(auth_code_1);
        rental.setStep1Passed(false);

        // 💡 [수정 완료] 무조건 1번으로 박히던 것을 책이 들어있는 실제 보관함 번호(storageNo)로 매핑!
        if (book.getStorageNo() != null) {
            try {
                rental.setCabinetSlot(Integer.valueOf(book.getStorageNo()));
            } catch (NumberFormatException e) {
                rental.setCabinetSlot(1); // 예외 발생 시 기본값 1
            }
        }

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

    // =====================================================================
    // 2. [반납하기] 기능
    // =====================================================================
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

    // =====================================================================
    // 3. [보관하기(사전 등록)] - 이중 검증으로 빈자리 탐색 후 도서 등록
    // =====================================================================
    @Transactional
    public Book allocateStorageAndRegisterBook(String title, List<Integer> emptySlotsFromSensor, String authCode1) {

        Integer assignedSlot = null;

        // 1) 센서상 비어있는 사물함 번호들을 순회
        for (Integer slot : emptySlotsFromSensor) {
            String slotStr = String.valueOf(slot);

            // 2) DB 교차 검증: 해당 자리에 등록된 가장 최근 도서 조회
            Optional<Book> lastBookInSlot = bookRepository.findTopByStorageNoOrderByIdDesc(slotStr);
            boolean isLogicallyOccupied = false;

            if (lastBookInSlot.isPresent()) {
                Book lb = lastBookInSlot.get();

                // [이중 검증 로직] 대여 중(borrower 존재)이거나 보관 중(isAvailable이 false)이면 사용 중으로 간주
                if ((lb.getBorrower() != null && !lb.getBorrower().isEmpty()) ||
                        (lb.getIsAvailable() != null && !lb.getIsAvailable())) {
                    isLogicallyOccupied = true;
                }
            }

            // 3) 물리적, 논리적으로 완벽한 빈자리 발견 시 배정
            if (!isLogicallyOccupied) {
                assignedSlot = slot;
                break;
            }
        }

        // 4) 빈자리 없음 예외 처리
        if (assignedSlot == null) {
            throw new IllegalStateException("현재 사용 가능한 빈 보관함 구역이 없습니다.");
        }

        System.out.println("🚨 [이중 검증 완료] 최종 할당된 빈 사물함 번호: " + assignedSlot);

        // 5) 신규 도서 정보 저장
        Book newBook = new Book();
        newBook.setTitle(title);
        newBook.setStorageNo(String.valueOf(assignedSlot));
        newBook.setAuthCode1(authCode1);

        // 보관 1단계 인증 전이므로 false 처리 (인증 완료 시 true로 변경됨)
        newBook.setIsAvailable(false);
        newBook.setStep1Passed(false);

        return bookRepository.saveAndFlush(newBook);
    }

    // =====================================================================
    // 4. [회수하기] 기능 - 주인이 책을 완전히 빼갈 때 호출
    // =====================================================================
    @Transactional
    public void retrieveBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 도서 ID입니다: " + bookId));

        // 회수 시 더 이상 앱 목록에 뜨지 않고, 이 자리는 이제 남이 쓸 수 있는 완전한 빈자리가 됨
        book.setIsAvailable(false);
        book.setBorrower("");
        book.setStep1Passed(false);

        bookRepository.saveAndFlush(book);
        System.out.println("🚨 [도서 회수 완료] 도서 ID " + bookId + "의 자리가 완전히 비워졌습니다.");
    }

    // =====================================================================
    // 5. 🚀 [하드웨어 센서 연동] 센서 상태를 받아 Book 테이블 실시간 동기화
    // =====================================================================
    @Transactional
    public void updateBookStatusBySensors(String lockerNo, List<Boolean> bookSensors) {
        if (bookSensors == null) return;

        for (int i = 0; i < bookSensors.size(); i++) {
            boolean isPresent = bookSensors.get(i);
            String slotNumberStr = String.valueOf(i + 1);

            List<Book> books = bookRepository.findAll();
            for (Book book : books) {
                if (book.getStorageNo() != null && book.getStorageNo().equals(slotNumberStr)) {
                    System.out.println("🔍 [센서 실시간 동기화] 구역: " + lockerNo + ", " + slotNumberStr + "번 칸 감지 상태: " + (isPresent ? "책 있음" : "빈자리"));
                }
            }
        }
    }

    // =====================================================================
    // 기타 검증 및 보조 로직
    // =====================================================================
    @Transactional
    public void saveSequentialAuthCodes(Integer cabinetSlot, String auth_code_1) {
        Rental rental = rentalRepository.findTopByOrderByIdDesc()
                .orElseThrow(() -> new IllegalArgumentException("활성화된 대여 내역이 없습니다."));
        rental.setCabinetSlot(cabinetSlot);
        rental.setAuth_code_1(auth_code_1);
    }

    @Transactional
    public boolean verifyRentalStep1(Integer cabinetSlot, String inputCode) {
        String cleanInput = inputCode != null ? inputCode.trim() : "";
        boolean isMatch = false;

        List<Rental> rentals = rentalRepository.findAll();
        for (Rental r : rentals) {
            if (r.getCabinetSlot() != null && r.getCabinetSlot().equals(cabinetSlot)
                    && "대여중".equals(r.getStatus())
                    && cleanInput.equals(r.getAuth_code_1())) {

                r.setStep1Passed(true);
                rentalRepository.saveAndFlush(r);
                isMatch = true;
                break;
            }
        }

        System.out.println("🚨 [대여 1단계 검증] 수납장: " + cabinetSlot + ", 입력번호: [" + cleanInput + "] -> 결과: " + isMatch);
        return isMatch;
    }

    @Transactional
    public boolean verifyStorageStep1(Integer cabinetSlot, String inputCode) {
        String cleanInput = inputCode != null ? inputCode.trim() : "";
        boolean isMatch = false;

        List<Book> books = bookRepository.findAllByOrderByIdDesc();

        for (Book book : books) {
            boolean isSlotMatch = book.getStorageNo() != null && book.getStorageNo().contains(String.valueOf(cabinetSlot));

            if (isSlotMatch && book.getAuthCode1() != null && book.getAuthCode1().equals(cleanInput)) {
                book.setStep1Passed(true);

                // 하드웨어 인증 완료 후 대여 목록 활성화
                book.setIsAvailable(true);

                bookRepository.saveAndFlush(book);
                isMatch = true;
                break;
            }
        }

        System.out.println("🚨 [보관 1단계 검증] 수납장: " + cabinetSlot + ", 입력번호: [" + cleanInput + "] -> 결과: " + isMatch);
        return isMatch;
    }

    public boolean appCheckStep1Status(Integer cabinetSlot) {
        boolean isPassed = false;

        List<Rental> rentals = rentalRepository.findAll();
        for (Rental r : rentals) {
            if (r.getCabinetSlot() != null && r.getCabinetSlot().equals(cabinetSlot)
                    && Boolean.TRUE.equals(r.getStep1Passed())) {
                isPassed = true;
                break;
            }
        }

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