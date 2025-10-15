// Import các module cần thiết của Firebase
const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

/**
 * Trigger này sẽ được kích hoạt mỗi khi có một bản ghi được
 * tạo, cập nhật, hoặc xóa trong đường dẫn /dataBookTable/{bookingId}.
 */
exports.calculateStoreStatistics = functions.database.ref("/dataBookTable/{bookingId}")
    .onWrite(async (change, context) => {
        // Lấy storeId từ đơn đặt bàn vừa thay đổi.
        // Nếu đơn hàng bị xóa, 'change.before' sẽ có dữ liệu.
        // Nếu đơn hàng mới/cập nhật, 'change.after' sẽ có dữ liệu.
        const data = change.after.exists() ? change.after.val() : change.before.val();
        const storeId = data.storeId;

        if (!storeId) {
            console.log("Không tìm thấy storeId trong đơn đặt bàn.");
            return null;
        }

        // Lấy thông tin về cửa hàng (để biết tổng số bàn)
        const storeSnap = await admin.database().ref(`/dataStore/${storeId}`).once("value");
        const storeData = storeSnap.val();
        const totalTables = Number(storeData.tableNumber) || 0;

        // Lấy ngày hôm nay theo định dạng "dd/MM/yyyy"
        const today = new Date().toLocaleDateString("vi-VN", {
            day: "2-digit",
            month: "2-digit",
            year: "numeric",
        });

        // Truy vấn tất cả các đơn đặt bàn của cửa hàng này trong ngày hôm nay
        const bookingsSnap = await admin.database().ref("/dataBookTable")
            .orderByChild("storeId")
            .equalTo(storeId)
            .once("value");

        // Bắt đầu tính toán
        let totalBookingsForDay = 0;
        let processingCount = 0;
        let confirmedCount = 0;
        let playingCount = 0;
        let totalRevenue = 0.0;
        let refusedCount = 0;

        bookingsSnap.forEach(bookingSnap => {
            const booking = bookingSnap.val();
            // Chỉ tính các đơn trong ngày hôm nay
            if (booking && booking.dateTime === today) {
                totalBookingsForDay++;
                switch (booking.status) {
                    case "Chờ xử lý":
                        processingCount++;
                        break;
                    case "Đã xác nhận":
                        confirmedCount++;
                        break;
                    case "Đang chơi":
                        confirmedCount++;
                        playingCount++;
                        break;
                    case "Đã hoàn thành":
                        confirmedCount++;
                        const moneyString = (booking.money || "").replace(/[^\d.]/g, "");
                        totalRevenue += Number(moneyString) || 0.0;
                        break;
                    case "Đã từ chối":
                        refusedCount++;
                        break;
                }
            }
        });

        const tableEmpty = totalTables - playingCount;

        // Chuẩn bị dữ liệu để cập nhật vào node /dataOverview
        const overviewData = {
            storeId: storeId,
            ownerId: storeData.ownerId,
            profit: totalRevenue,
            tableEmpty: tableEmpty,
            pendingBookings: processingCount,
            confirm: confirmedCount,
            tableActive: playingCount,
            totalBooking: totalBookingsForDay,
            maintenance: refusedCount,
        };

        console.log(`Cập nhật thống kê cho cửa hàng ${storeId}:`, overviewData);

        // Ghi dữ liệu đã tính toán vào Realtime Database
        return admin.database().ref(`/dataOverview/${storeId}`).update(overviewData);
    });

