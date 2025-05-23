package io.hhplus.tdd.point;

import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.database.PointHistoryTable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class MockDataInitializerTest {

    @Autowired
    private UserPointTable userPointTable;

    @Autowired
    private PointHistoryTable pointHistoryTable;

    @Test
    public void testMockDataAddedToUserPointTable() {
        UserPoint user1 = userPointTable.selectById(1L);
        UserPoint user2 = userPointTable.selectById(2L);

        assertNotNull(user1, "User 1 should exist in UserPointTable");

        assertNotNull(user2, "User 2 should exist in UserPointTable");
    }

    @Test
    public void testMockDataAddedToPointHistoryTable() {
        List<PointHistory> user1Histories = pointHistoryTable.selectAllByUserId(1L);
        List<PointHistory> user2Histories = pointHistoryTable.selectAllByUserId(2L);

        assertEquals(1, user1Histories.size(), "User 1 should have 1 history record");
        assertEquals(1, user2Histories.size(), "User 2 should have 1 history record");

    }
}