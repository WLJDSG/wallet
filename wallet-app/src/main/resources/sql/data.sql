-- 种子数据：两张优惠券模板（联调用）
INSERT INTO coupon (name, face_amount, min_amount, total_count, taken_count, expire_time, status) VALUES
('满100减10', 1000, 10000, 100, 0, '2099-12-31 23:59:59', 1),
('满50减5', 500, 5000, 100, 0, '2099-12-31 23:59:59', 1);
