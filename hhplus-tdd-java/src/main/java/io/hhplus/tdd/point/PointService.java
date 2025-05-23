package io.hhplus.tdd.point;

import io.hhplus.tdd.ErrorMessages;
import java.util.List;
import java.util.Optional;

public class PointService {
    private final PointRepository pointRepository;

    public PointService(PointRepository pointRepository) {
        this.pointRepository = pointRepository;
    }

    public UserPoint point(long userId) {
        return pointRepository.findById(userId);
    }

    public List<PointHistory> history(long userId) {
        return pointRepository.findHistoryByUserId(userId);
    }

    public UserPoint charge(long userId, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException(ErrorMessages.CHARGE_AMOUNT_POSITIVE);
        }
        UserPoint userPoint = pointRepository.findById(userId);
        userPoint = new UserPoint(userPoint.id(), userPoint.point() + amount, System.currentTimeMillis());
        pointRepository.save(userPoint);
        pointRepository.saveHistory(userPoint.id(), amount, TransactionType.CHARGE, System.currentTimeMillis());
        return userPoint;
    }

    public UserPoint use(long userId, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException(ErrorMessages.USE_AMOUNT_POSITIVE);
        }
        UserPoint userPoint = pointRepository.findById(userId);
        if (userPoint.point() < amount) {
            throw new IllegalArgumentException(ErrorMessages.INSUFFICIENT_POINTS);
        }
        userPoint = new UserPoint(userPoint.id(), userPoint.point() - amount, System.currentTimeMillis());
        pointRepository.save(userPoint);
        pointRepository.saveHistory(userPoint.id(), -amount, TransactionType.USE, System.currentTimeMillis());
        return userPoint;
    }
}
