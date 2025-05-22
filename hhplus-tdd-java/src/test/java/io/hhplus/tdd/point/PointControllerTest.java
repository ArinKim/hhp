package io.hhplus.tdd.point;

import io.hhplus.tdd.ErrorMessages;
import io.hhplus.tdd.database.PointHistoryTable;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;

public class PointControllerTest {

    private PointController pointController;
    private UserPointTable userPointTable;
    private PointHistoryTable pointHistoryTable;

    @BeforeEach
    void setUp() {
        // Create mock for UserPointTable
        userPointTable = mock(UserPointTable.class);
        pointHistoryTable = mock(PointHistoryTable.class);
        // Initialize PointController with the mock
        pointController = new PointController(userPointTable, pointHistoryTable);
    }

    @Test
    // Test to check if the point is 0 when accidentally assigned negative value for the point
    public void UserPoint_cannotBeNegative() {
        // given
        long testId = 1L;
        int testPoint = -100; //insertOrUpdate

        assertThatThrownBy(() -> {
            pointController.charge(testId, testPoint);
        }).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining(ErrorMessages.CHARGE_AMOUNT_POSITIVE);
    }

    @Test
    public void UserPoint_useSuccess() {
        // given
        long testId = 1L;
        long currentPoint = 500L;
        long useAmount = 200L;
        UserPoint beforeUse = new UserPoint(testId, currentPoint, System.currentTimeMillis());
        UserPoint afterUse = new UserPoint(testId, currentPoint - useAmount, System.currentTimeMillis());

        when(userPointTable.selectById(testId)).thenReturn(beforeUse);
        when(userPointTable.insertOrUpdate(testId, -useAmount)).thenReturn(afterUse);

        // when
        UserPoint result = pointController.use(testId, useAmount);

        // then
        assertNotNull(result);
        assertEquals(currentPoint - useAmount, result.point());
    }

    @Test
    public void UserPoint_useFail() {
        // given
        long testId = 1L;
        long currentPoint = 500L;
        long useAmount = 600L; // More than current point
        UserPoint beforeUse = new UserPoint(testId, currentPoint, System.currentTimeMillis());

        when(userPointTable.selectById(testId)).thenReturn(beforeUse);

        // when
        assertThatThrownBy(() -> {
            pointController.use(testId, useAmount);
        }).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining(ErrorMessages.INSUFFICIENT_POINTS);
    }

    @Test
    public void UserPoint_useZeroPointFail() {
        // given
        long testId = 1L;
        long currentPoint = 500L;
        long useAmount = 0L; // Using zero points
        UserPoint beforeUse = new UserPoint(testId, currentPoint, System.currentTimeMillis());

        when(userPointTable.selectById(testId)).thenReturn(beforeUse);

        // when
        assertThatThrownBy(() -> {
            pointController.use(testId, useAmount);
        }).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining(ErrorMessages.USE_AMOUNT_POSITIVE);
    }

    @Test
    public void PointHistory_insertSuccessForCharge() {
        // given
        long testId = 1L;
        long amount = 100L;
        TransactionType transactionType = TransactionType.CHARGE;

        // when
        pointController.charge(testId, amount);

        // then
        verify(pointHistoryTable).insert(eq(testId), eq(amount), eq(transactionType), anyLong());
    }

    @Test
    public void PointHistory_insertSuccessForUse() {
        // given
        long testId = 1L;
        long amount = 100L;
        long currentPoint = 500L;
        TransactionType transactionType = TransactionType.USE;
        UserPoint beforeUse = new UserPoint(testId, currentPoint, System.currentTimeMillis());

        when(userPointTable.selectById(testId)).thenReturn(beforeUse);

        // when
        pointController.use(testId, amount);

        // then
        verify(pointHistoryTable).insert(eq(testId), eq(-amount), eq(transactionType), anyLong());

    }
}