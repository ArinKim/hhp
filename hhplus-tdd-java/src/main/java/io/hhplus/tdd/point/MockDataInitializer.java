package io.hhplus.tdd.point;

import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.point.TransactionType;
import org.springframework.stereotype.Component;

@Component
public class MockDataInitializer {
    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    public MockDataInitializer(UserPointTable userPointTable, PointHistoryTable pointHistoryTable) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    public void init() {
        // Add mock user points
        userPointTable.insertOrUpdate(1L, 1000L);
        userPointTable.insertOrUpdate(2L, 500L);

        // Add mock point histories
        pointHistoryTable.insert(1L, 1000L, TransactionType.CHARGE, System.currentTimeMillis());
        pointHistoryTable.insert(2L, 500L, TransactionType.CHARGE, System.currentTimeMillis());
    }
}