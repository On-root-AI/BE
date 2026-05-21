package com.OnRoot.onroot.domain.dday.service;

import com.OnRoot.onroot.domain.dday.dto.DDayRequest;
import com.OnRoot.onroot.domain.dday.dto.DDayResponse;
import com.OnRoot.onroot.domain.dday.entity.DDay;
import com.OnRoot.onroot.domain.dday.repository.DDayRepository;
import com.OnRoot.onroot.domain.user.entity.User;
import com.OnRoot.onroot.global.exception.NotFoundException;
import com.OnRoot.onroot.global.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DDayService {

    private final DDayRepository ddayRepository;

    @Transactional
    public DDayResponse createDDay(DDayRequest request, User user) {
        DDay dday = DDay.builder()
                .user(user)
                .title(request.title())
                .targetDate(request.targetDate())
                .createdAt(LocalDateTime.now())
                .build();
        return DDayResponse.from(ddayRepository.save(dday));
    }

    @Transactional(readOnly = true)
    public List<DDayResponse> getDDays(User user) {
        return ddayRepository.findByUserOrderByTargetDateAsc(user).stream()
                .map(DDayResponse::from)
                .toList();
    }

    @Transactional
    public DDayResponse updateDDay(Long ddayId, DDayRequest request, User user) {
        DDay dday = getDDayAndCheckOwnership(ddayId, user);
        dday.update(request.title(), request.targetDate());
        return DDayResponse.from(dday);
    }

    @Transactional
    public void deleteDDay(Long ddayId, User user) {
        DDay dday = getDDayAndCheckOwnership(ddayId, user);
        ddayRepository.delete(dday);
    }

    private DDay getDDayAndCheckOwnership(Long ddayId, User user) {
        DDay dday = ddayRepository.findById(ddayId)
                .orElseThrow(() -> new NotFoundException("D-day를 찾을 수 없습니다."));
        if (!dday.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("해당 리소스에 접근 권한이 없습니다.");
        }
        return dday;
    }
}
