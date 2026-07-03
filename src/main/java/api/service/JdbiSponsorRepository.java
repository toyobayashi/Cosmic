package api.service;

import api.model.dto.SponsorOrderDTO;
import org.jdbi.v3.core.Handle;
import tools.DatabaseConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JdbiSponsorRepository implements SponsorRepository {

    @Override
    public SponsorOrderDTO createOrder(int accountId, int amountCents, int expectedNx, String note) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            String orderId = SponsorOrderIdGenerator.nextOrderId();
            handle.createUpdate("""
                            INSERT INTO sponsor_orders (order_id, account, amount_cents, expected_nx, note)
                            SELECT ?, name, ?, ?, ? FROM accounts WHERE id = ?
                            """)
                    .bind(0, orderId)
                    .bind(1, amountCents)
                    .bind(2, expectedNx)
                    .bind(3, note)
                    .bind(4, accountId)
                    .execute();
            return getOrder(handle, orderId);
        }
    }

    @Override
    public List<SponsorOrderDTO> listOrders(String account, String status) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            StringBuilder sql = new StringBuilder(baseSelect() + " WHERE 1 = 1");
            List<Object> params = new ArrayList<>();
            if (account != null) {
                sql.append(" AND o.account = ?");
                params.add(account);
            }
            if (status != null && !status.isBlank()) {
                sql.append(" AND o.status = ?");
                params.add(status);
            }
            sql.append(" ORDER BY o.created_at DESC, o.id DESC");

            var query = handle.createQuery(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                query.bind(i, params.get(i));
            }
            return query.mapToMap().stream().map(this::mapOrder).toList();
        }
    }

    @Override
    public List<SponsorOrderDTO> listOrdersByAccountId(int accountId, String status) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            StringBuilder sql = new StringBuilder(baseSelect() + " WHERE a.id = ?");
            List<Object> params = new ArrayList<>();
            params.add(accountId);
            if (status != null && !status.isBlank()) {
                sql.append(" AND o.status = ?");
                params.add(status);
            }
            sql.append(" ORDER BY o.created_at DESC, o.id DESC");

            var query = handle.createQuery(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                query.bind(i, params.get(i));
            }
            return query.mapToMap().stream().map(this::mapOrder).toList();
        }
    }

    @Override
    public SponsorOrderDTO awardPendingOrder(String orderId, int awardedNx, int awardedBy) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            return handle.inTransaction(transaction -> {
                SponsorOrderDTO order = transaction.createQuery(baseSelect() + " WHERE o.order_id = ? FOR UPDATE")
                        .bind(0, orderId)
                        .mapToMap()
                        .findOne()
                        .map(this::mapOrder)
                        .orElseThrow(() -> new IllegalArgumentException("Sponsor order not found: " + orderId));

                if (!SponsorService.STATUS_PENDING.equals(order.getStatus())) {
                    throw new IllegalArgumentException("Sponsor order has already been awarded");
                }

                transaction.createUpdate("UPDATE accounts SET nxCredit = COALESCE(nxCredit, 0) + ? WHERE name = ?")
                        .bind(0, awardedNx)
                        .bind(1, order.getAccount())
                        .execute();

                int updated = transaction.createUpdate("""
                                UPDATE sponsor_orders
                                SET status = 'AWARDED', awarded_nx = ?, updated_at = CURRENT_TIMESTAMP, updated_by = ?
                                WHERE order_id = ? AND status = 'PENDING'
                                """)
                        .bind(0, awardedNx)
                        .bind(1, awardedBy)
                        .bind(2, orderId)
                        .execute();
                if (updated != 1) {
                    throw new IllegalArgumentException("Sponsor order has already been awarded");
                }
                return getOrder(transaction, orderId);
            });
        }
    }

    @Override
    public SponsorOrderDTO closePendingOrder(String orderId, int closedBy) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            return handle.inTransaction(transaction -> {
                SponsorOrderDTO order = transaction.createQuery(baseSelect() + " WHERE o.order_id = ? FOR UPDATE")
                        .bind(0, orderId)
                        .mapToMap()
                        .findOne()
                        .map(this::mapOrder)
                        .orElseThrow(() -> new IllegalArgumentException("Sponsor order not found: " + orderId));

                if (!SponsorService.STATUS_PENDING.equals(order.getStatus())) {
                    throw new IllegalArgumentException("Only pending sponsor orders can be closed");
                }

                int updated = transaction.createUpdate("""
                                UPDATE sponsor_orders
                                SET status = 'CLOSED', updated_at = CURRENT_TIMESTAMP, updated_by = ?
                                WHERE order_id = ? AND status = 'PENDING'
                                """)
                        .bind(0, closedBy)
                        .bind(1, orderId)
                        .execute();
                if (updated != 1) {
                    throw new IllegalArgumentException("Only pending sponsor orders can be closed");
                }
                return getOrder(transaction, orderId);
            });
        }
    }

    private SponsorOrderDTO getOrder(Handle handle, String orderId) {
        return handle.createQuery(baseSelect() + " WHERE o.order_id = ?")
                .bind(0, orderId)
                .mapToMap()
                .findOne()
                .map(this::mapOrder)
                .orElseThrow(() -> new IllegalArgumentException("Sponsor order not found: " + orderId));
    }

    private String baseSelect() {
        return """
                SELECT o.order_id, o.account, a.id AS internal_account_id, o.amount_cents, o.expected_nx,
                       o.awarded_nx, o.status, o.note, o.created_at, o.updated_at, o.updated_by
                FROM sponsor_orders o
                JOIN accounts a ON a.name = o.account
                """;
    }

    private SponsorOrderDTO mapOrder(Map<String, Object> row) {
        SponsorOrderDTO order = new SponsorOrderDTO();
        order.setOrderId(toString(row.get("order_id")));
        order.setAccount(toString(row.get("account")));
        order.setInternalAccountId(toInt(row.get("internal_account_id")));
        order.setAmountCents(toInt(row.get("amount_cents")));
        order.setExpectedNx(toInt(row.get("expected_nx")));
        order.setAwardedNx(toInt(row.get("awarded_nx")));
        order.setStatus(toString(row.get("status")));
        order.setNote(toString(row.get("note")));
        order.setCreatedAt(toString(row.get("created_at")));
        order.setUpdatedAt(toString(row.get("updated_at")));
        order.setUpdatedBy(toInt(row.get("updated_by")));
        return order;
    }

    private Integer toInt(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private String toString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
