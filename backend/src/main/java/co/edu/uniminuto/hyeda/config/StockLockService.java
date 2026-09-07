package co.edu.uniminuto.hyeda.config;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Product locks for concurrent stock reservation.
 * In-memory ReentrantLock (production can swap for Redis-backed locks).
 */
@Service
public class StockLockService {

    private final Map<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

    public void lock(Long productId) {
        locks.computeIfAbsent(productId, id -> new ReentrantLock()).lock();
    }

    public void unlock(Long productId) {
        ReentrantLock lock = locks.get(productId);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
