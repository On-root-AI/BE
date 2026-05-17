package com.OnRoot.onroot.domain.dday.service;

import com.OnRoot.onroot.domain.dday.dto.DDayRequest;
import com.OnRoot.onroot.domain.dday.dto.DDayResponse;
import com.OnRoot.onroot.domain.dday.entity.DDay;
import com.OnRoot.onroot.domain.dday.repository.DDayRepository;
import com.OnRoot.onroot.domain.user.entity.User;
import com.OnRoot.onroot.domain.user.repository.UserRepository;
import com.OnRoot.onroot.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DDayService {

    private final DDayRepository ddayRepository;
    private final UserRepository userRepository;

    @Transactional
    public DDayResponse createDDay(DDayRequest request) {
        User user = getDummyUser();
        DDay dday = DDay.builder()
                .user(user)
                .title(request.title())
                .targetDate(request.targetDate())
                .createdAt(LocalDateTime.now())
                .build();
        return DDayResponse.from(ddayRepository.save(dday));
    }

    @Transactional(readOnly = true)
    public List<DDayResponse> getDDays() {
        User user = getDummyUser();
        return ddayRepository.findByUserOrderByTargetDateAsc(user).stream()
                .map(DDayResponse::from)
                .toList();
    }

    @Transactional
    public DDayResponse updateDDay(Long ddayId, DDayRequest request) {
        DDay dday = ddayRepository.findById(ddayId)
                .orElseThrow(() -> new NotFoundException("D-day를 찾을 수 없습니다."));
        dday.update(request.title(), request.targetDate());
        return DDayResponse.from(dday);
    }

    @Transactional
    public void deleteDDay(Long ddayId) {
        DDay dday = ddayRepository.findById(ddayId)
                .orElseThrow(() -> new NotFoundException("D-day를 찾을 수 없습니다."));
        ddayRepository.delete(dday);
    }

    private User getDummyUser() {
        return userRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException("더미 유저가 없습니다."));
    }
}
