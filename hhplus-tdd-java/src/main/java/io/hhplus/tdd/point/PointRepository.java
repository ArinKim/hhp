package io.hhplus.tdd.point;

import java.util.ArrayList;
import java.util.List;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.database.PointHistoryTable;
import java.util.Optional;

public class PointRepository {
    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    public PointRepository(UserPointTable userPointTable, PointHistoryTable pointHistoryTable) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    public void save(UserPoint userPoint) {
        userPointTable.insertOrUpdate(userPoint.id(), userPoint.point());
    }

    public UserPoint findById(long userId) {
        return userPointTable.selectById(userId);
    }

    // Add methods for point history
     public void saveHistory(long id, long point, TransactionType type, long updateMillis) {
        pointHistoryTable.insert(id, point, type, updateMillis);
     }

     public List<PointHistory> findHistoryByUserId(long userId) {
        return pointHistoryTable.selectAllByUserId(userId);
     }
}