package io.hhplus.tdd.point;

import io.hhplus.tdd.ErrorMessages;
import io.hhplus.tdd.database.PointHistoryTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    @DisplayName("UserPoint cannot be negative")
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
    @DisplayName("UserPoint charge success")
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
    @DisplayName("UserPoint use fail when using more points than available")
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
    @DisplayName("UserPoint use fail when using zero points")
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
    @DisplayName("PointHistory insert success for charge")
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
    @DisplayName("PointHistory insert success for use")
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

    @Test
    @DisplayName("Concurrent point use should update safely")
    public void concurrentPointUse_shouldUpdateSafely() throws InterruptedException {
        // given
        long testId = 1L;
        long initialPoint = 1000L;
        long useAmount = 100L;
        int threadCount = 5;

        UserPointTable userPointTable = mock(UserPointTable.class);
        PointHistoryTable pointHistoryTable = mock(PointHistoryTable.class);
        PointController pointController = new PointController(userPointTable, pointHistoryTable);

        // Mock selectById and insertOrUpdate to simulate point deduction
        // Use an array to hold the current point for thread-safe updates
        final long[] currentPoint = {initialPoint};

        when(userPointTable.selectById(testId)).thenAnswer(invocation -> new UserPoint(testId, currentPoint[0], System.currentTimeMillis()));
        when(userPointTable.insertOrUpdate(eq(testId), anyLong())).thenAnswer(invocation -> {
            long delta = invocation.getArgument(1);
            currentPoint[0] += delta;
            return new UserPoint(testId, currentPoint[0], System.currentTimeMillis());
        });

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    pointController.use(testId, useAmount);
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Wait for all threads to be ready
        readyLatch.await();
        // Start all threads at once
        startLatch.countDown();
        // Wait for all threads to finish
        doneLatch.await();
        executor.shutdown();

        // then
        // Each thread should have deducted useAmount
        long expected = initialPoint - (useAmount * threadCount);
        assertEquals(expected, currentPoint[0]);
    }

    @Test
    @DisplayName("Only 4 out of 10 concurrent point uses succeed, rest fail due to insufficient points")
    public void concurrentPointUse_partialSuccessAndFailures() throws InterruptedException {
        long testId = 1L;
        long initialPoint = 400L;
        long useAmount = 100L;
        int threadCount = 10;

        UserPointTable userPointTable = mock(UserPointTable.class);
        PointHistoryTable pointHistoryTable = mock(PointHistoryTable.class);
        PointController pointController = new PointController(userPointTable, pointHistoryTable);

        final long[] currentPoint = {initialPoint};

        when(userPointTable.selectById(testId)).thenAnswer(invocation -> new UserPoint(testId, currentPoint[0], System.currentTimeMillis()));
        when(userPointTable.insertOrUpdate(eq(testId), anyLong())).thenAnswer(invocation -> {
            long delta = invocation.getArgument(1);
            currentPoint[0] += delta;
            return new UserPoint(testId, currentPoint[0], System.currentTimeMillis());
        });

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger failCount = new java.util.concurrent.atomic.AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    pointController.use(testId, useAmount);
                    successCount.incrementAndGet();
                } catch (IllegalArgumentException e) {
                    failCount.incrementAndGet();
                    assertTrue(e.getMessage().contains(ErrorMessages.INSUFFICIENT_POINTS));
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertEquals(4, successCount.get());
        assertEquals(6, failCount.get());
        assertEquals(0, currentPoint[0]);
    }

    @Test
    @DisplayName("Concurrent point charge should update safely")
    public void concurrentPointCharge_shouldUpdateSafely() throws InterruptedException {
        // given
        long testId = 1L;
        long initialPoint = 100L;
        long addAmount = 10L;
        int threadCount = 10;

        UserPointTable userPointTable = mock(UserPointTable.class);
        PointHistoryTable pointHistoryTable = mock(PointHistoryTable.class);
        PointController pointController = new PointController(userPointTable, pointHistoryTable);

        // Mock selectById and insertOrUpdate to simulate point deduction
        // Use an array to hold the current point for thread-safe updates
        final long[] currentPoint = {initialPoint};

        when(userPointTable.selectById(testId)).thenAnswer(invocation -> new UserPoint(testId, currentPoint[0], System.currentTimeMillis()));
        when(userPointTable.insertOrUpdate(eq(testId), anyLong())).thenAnswer(invocation -> {
            long delta = invocation.getArgument(1);
            currentPoint[0] += delta;
            return new UserPoint(testId, currentPoint[0], System.currentTimeMillis());
        });

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    pointController.charge(testId, addAmount);
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Wait for all threads to be ready
        readyLatch.await();
        // Start all threads at once
        startLatch.countDown();
        // Wait for all threads to finish
        doneLatch.await();
        executor.shutdown();

        // then
        // Each thread should have deducted addAmount
        long expected = initialPoint + (addAmount * threadCount);
        assertEquals(expected, currentPoint[0]);
    }
}