//================================================================================
// KHAI BÁO THƯ VIỆN
//================================================================================
const express = require('express');
const moment = require('moment');
const crypto = require('crypto');
const querystring = require('qs');
const admin = require('firebase-admin');
const axios = require('axios'); // Thêm axios để gọi API VietQR

const app = express();
const port = process.env.PORT || 3000;

app.use(express.json());

//================================================================================
// KHỞI TẠO FIREBASE ADMIN SDK (CHỈ MỘT LẦN)
//================================================================================
try {
    const serviceAccount = JSON.parse(process.env.FIREBASE_CREDENTIALS);
    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount),
      databaseURL: process.env.FIREBASE_DATABASE_URL
    });
    console.log("Firebase Admin SDK initialized successfully.");
} catch (error) {
    console.error("!!! LỖI KHỞI TẠO FIREBASE ADMIN SDK !!!:", error);
    process.exit(1); // Thoát nếu không khởi tạo được Firebase
}

// Lấy tham chiếu cho CẢ HAI database
const db = admin.database(); // Realtime Database
const dbFirestore = admin.firestore(); // Firestore (để cập nhật role)

//================================================================================
// CẤU HÌNH BIẾN MÔI TRƯỜNG (CHỈ MỘT LẦN)
//================================================================================
// --- Cấu hình VNPAY ---
const tmnCode = process.env.VNP_TMNCODE;
const secretKey = process.env.VNP_HASHSECRET; // Quan trọng: Đảm bảo biến này đúng trên Render
let vnpUrl = process.env.VNP_URL || 'https://sandbox.vnpayment.vn/paymentv2/vpcpay.html';
const returnUrl = process.env.VNP_RETURNURL;
const frontendSuccessUrl = process.env.FRONTEND_SUCCESS_URL;
const frontendFailUrl = process.env.FRONTEND_FAIL_URL;

// --- Cấu hình VietQR ---
const VIETQR_API_URL = process.env.VIETQR_API_URL;
const VIETQR_API_KEY = process.env.VIETQR_API_KEY;
const VIETQR_IPN_SECRET = process.env.VIETQR_IPN_SECRET;
const VIETQR_MERCHANT_ACC_NO = process.env.VIETQR_MERCHANT_ACC_NO;
const VIETQR_MERCHANT_BIN = process.env.VIETQR_MERCHANT_BIN;
const VIETQR_MERCHANT_NAME = process.env.VIETQR_MERCHANT_NAME;

// Kiểm tra các biến môi trường quan trọng
if (!tmnCode || !secretKey || !returnUrl || !process.env.FIREBASE_CREDENTIALS || !process.env.FIREBASE_DATABASE_URL) {
    console.error("!!! LỖI: Thiếu các biến môi trường VNPAY hoặc Firebase quan trọng!");
    process.exit(1);
}

//================================================================================
// HÀM HỖ TRỢ SẮP XẾP (Dùng cho VNPAY)
//================================================================================
function sortObject(obj) {
    let sorted = {};
    let str = [];
    for (let key in obj) {
        if (obj.hasOwnProperty(key)) {
            str.push(encodeURIComponent(key));
        }
    }
    str.sort();
    for (let key = 0; key < str.length; key++) {
        sorted[str[key]] = encodeURIComponent(obj[str[key]]).replace(/%20/g, "+");
    }
    return sorted;
}

//================================================================================
// API ENDPOINTS (Tạo Link Thanh Toán, Nhận IPN, ...)
//================================================================================

// ENDPOINT 1: TẠO LINK THANH TOÁN VNPAY (ĐẶT BÀN)
app.post('/create_payment_url', (req, res) => {
    try {
        const date = new Date();
        const createDate = moment(date).format('YYYYMMDDHHmmss');
        const ipAddr = req.headers['x-forwarded-for'] || req.connection.remoteAddress || req.socket.remoteAddress;
        const { amount, bankCode, language, orderId } = req.body;
        if (!amount || !orderId) { return res.status(400).json({ error: 'Missing required fields: amount, orderId' }); }
        const orderInfo = `Thanh toan don hang ${orderId}`;
        const orderType = 'other';
        const locale = language || 'vn';
        const currCode = 'VND';
        let vnp_Params = {
            'vnp_Version': '2.1.0', 'vnp_Command': 'pay', 'vnp_TmnCode': tmnCode,
            'vnp_Locale': locale, 'vnp_CurrCode': currCode, 'vnp_TxnRef': orderId,
            'vnp_OrderInfo': orderInfo, 'vnp_OrderType': orderType, 'vnp_Amount': amount * 100,
            'vnp_ReturnUrl': returnUrl, 'vnp_IpAddr': ipAddr, 'vnp_CreateDate': createDate
        };
        if (bankCode) vnp_Params['vnp_BankCode'] = bankCode;
        vnp_Params = sortObject(vnp_Params);
        const signData = querystring.stringify(vnp_Params, { encode: false });
        const hmac = crypto.createHmac("sha512", secretKey);
        const signed = hmac.update(Buffer.from(signData, 'utf-8')).digest("hex");
        vnp_Params['vnp_SecureHash'] = signed;
        const paymentUrl = vnpUrl + '?' + querystring.stringify(vnp_Params, { encode: false });
        console.log(`[VNPAY Create] Tao URL thanh cong cho don dat ban ${orderId}`);
        res.json({ paymentUrl });
    } catch (error) {
        console.error(`[VNPAY Create] Loi khi tao URL cho don dat ban:`, error);
        res.status(500).json({ error: 'Internal server error while creating VNPAY URL.' });
    }
});

// ENDPOINT 2: TẠO LINK THANH TOÁN VNPAY (NÂNG CẤP)
app.post('/create_upgrade_payment_url', (req, res) => {
    // ... (Code endpoint này giữ nguyên như cũ) ...
     try {
        const date = new Date();
        const createDate = moment(date).format('YYYYMMDDHHmmss');
        const ipAddr = req.headers['x-forwarded-for'] || req.connection.remoteAddress || req.socket.remoteAddress;
        const { amount, userId, storeId } = req.body;
        if (!amount || !userId || !storeId) { return res.status(400).json({ error: 'Missing required fields: amount, userId, storeId' }); }
        const orderId = `UPGRADE_${userId}_${storeId}_${createDate}`;
        const orderInfo = `Thanh toan nang cap ${userId}`;
        const orderType = 'other';
        const locale = 'vn';
        const currCode = 'VND';
        let vnp_Params = {
            'vnp_Version': '2.1.0', 'vnp_Command': 'pay', 'vnp_TmnCode': tmnCode,
            'vnp_Locale': locale, 'vnp_CurrCode': currCode, 'vnp_TxnRef': orderId,
            'vnp_OrderInfo': orderInfo, 'vnp_OrderType': orderType, 'vnp_Amount': amount * 100,
            'vnp_ReturnUrl': returnUrl, 'vnp_IpAddr': ipAddr, 'vnp_CreateDate': createDate
        };
        vnp_Params = sortObject(vnp_Params);
        const signData = querystring.stringify(vnp_Params, { encode: false });
        const hmac = crypto.createHmac("sha512", secretKey);
        const signed = hmac.update(Buffer.from(signData, 'utf-8')).digest("hex");
        vnp_Params['vnp_SecureHash'] = signed;
        const paymentUrl = vnpUrl + '?' + querystring.stringify(vnp_Params, { encode: false });
        console.log(`[VNPAY Create] Tao URL thanh cong cho don nang cap ${orderId}`);
        res.json({ paymentUrl });
    } catch (error) {
        console.error(`[VNPAY Create] Loi khi tao URL cho don nang cap:`, error);
        res.status(500).json({ error: 'Internal server error while creating VNPAY upgrade URL.' });
    }
});

// ENDPOINT 3: TẠO CHUỖI DỮ LIỆU VIETQR
app.post('/create_vietqr_data', async (req, res) => {
    const { amount, orderId } = req.body;
    if (!amount || !orderId) { return res.status(400).json({ error: 'Missing required fields: amount, orderId' }); }
    if (!VIETQR_API_URL || !VIETQR_API_KEY || !VIETQR_MERCHANT_ACC_NO || !VIETQR_MERCHANT_BIN) {
         console.error('[VietQR] Lỗi: Thiếu cấu hình biến môi trường VietQR.');
         return res.status(500).json({ error: 'VietQR service is not configured.'});
    }
    try {
        const vietQrApiPayload = {
            accountNo: VIETQR_MERCHANT_ACC_NO,
            accountName: VIETQR_MERCHANT_NAME,
            acqId: VIETQR_MERCHANT_BIN,
            amount: amount, addInfo: orderId,
            format: "text", template: "compact"
        };
        console.log('[VietQR] Dang goi API:', VIETQR_API_URL, 'Payload:', vietQrApiPayload);
        // *** SỬA HEADER XÁC THỰC ***
        const apiResponse = await axios.post(VIETQR_API_URL, vietQrApiPayload, {
            headers: { 'x-api-key': VIETQR_API_KEY, 'Content-Type': 'application/json' }
        });
        // *** SỬA CÁCH LẤY DATA ***
        if (apiResponse.data && apiResponse.data.code === '00' && apiResponse.data.data && apiResponse.data.data.qrDataURL) {
             const qrDataString = apiResponse.data.data.qrDataURL;
             console.log(`[VietQR] Tao chuoi QR thanh cong cho ${orderId}: ${qrDataString.substring(0, 50)}...`);
             res.json({ qrDataString: qrDataString });
        } else {
             console.error('[VietQR] Loi tu API VietQR (khong thanh cong):', apiResponse.data);
             res.status(500).json({ error: 'Lỗi khi tạo mã VietQR từ nhà cung cấp.' });
        }
    } catch (error) {
        console.error('[VietQR] Loi ngoai le khi goi API VietQR:', error.response ? JSON.stringify(error.response.data) : error.message);
        res.status(500).json({ error: 'Lỗi hệ thống khi gọi API VietQR.' });
    }
});

// ENDPOINT 4: VNPAY RETURN URL (Chỉ chuyển hướng)
app.get('/vnpay_return', function (req, res, next) {
    try {
        let vnp_Params = req.query;
        const secureHash = vnp_Params['vnp_SecureHash'];
        delete vnp_Params['vnp_SecureHash']; delete vnp_Params['vnp_SecureHashType'];
        vnp_Params = sortObject(vnp_Params);
        const signData = querystring.stringify(vnp_Params, { encode: false });
        const hmac = crypto.createHmac("sha512", secretKey);
        const signed = hmac.update(Buffer.from(signData, 'utf-8')).digest("hex");
        if (secureHash === signed) {
            console.log(`[VNPAY Return] Thanh cong cho order ${vnp_Params['vnp_TxnRef']}`);
            res.redirect(frontendSuccessUrl || '/');
        } else {
            console.log(`[VNPAY Return] That bai (sai hash) cho order ${vnp_Params['vnp_TxnRef']}`);
            res.redirect(frontendFailUrl || '/');
        }
    } catch (error) {
        console.error('[VNPAY Return] Loi xu ly:', error);
        res.redirect(frontendFailUrl || '/');
    }
});

// ENDPOINT 5: VNPAY IPN URL (Cập nhật DB cho VNPAY)
app.get('/vnpay_ipn', async function (req, res, next) {
    // ... (Code endpoint này giữ nguyên như cũ) ...
    try {
        let vnp_Params = req.query;
        let secureHash = vnp_Params['vnp_SecureHash'];
        delete vnp_Params['vnp_SecureHash']; delete vnp_Params['vnp_SecureHashType'];
        vnp_Params = sortObject(vnp_Params);
        let signData = querystring.stringify(vnp_Params, { encode: false });
        let hmac = crypto.createHmac("sha512", secretKey);
        let signed = hmac.update(Buffer.from(signData, 'utf-8')).digest("hex");

        if (secureHash !== signed) { console.log('[VNPAY IPN] Lỗi: Invalid Checksum'); return res.status(200).json({ RspCode: '97', Message: 'Invalid Checksum' }); }

        const orderId = vnp_Params['vnp_TxnRef'];
        const rspCode = vnp_Params['vnp_ResponseCode'];
        const vnpAmount = parseInt(vnp_Params['vnp_Amount']) / 100;
        console.log(`[VNPAY IPN] Nhan thong bao cho order: ${orderId}, Ma loi: ${rspCode}, So tien: ${vnpAmount}`);

        if (orderId.startsWith('BIDA')) {
            const orderRef = db.ref(`dataBookTable/${orderId}`);
            const snapshot = await orderRef.once('value');
            if (!snapshot.exists()) { console.warn(`[VNPAY IPN] Don dat ban ${orderId} khong tim thay.`); return res.status(200).json({ RspCode: '01', Message: 'Order not found' }); }
            const orderData = snapshot.val();
            if (orderData.paymentStatus !== 'Chờ thanh toán VNPay') { console.log(`[VNPAY IPN] Don dat ban ${orderId} da duoc xu ly truoc do (${orderData.paymentStatus}). Bo qua.`); return res.status(200).json({ RspCode: '02', Message: 'Order already confirmed' }); }
            // Thêm kiểm tra số tiền nếu cần
            let paymentStatus = (rspCode == '00') ? 'Đã thanh toán' : 'FAILED';
            await orderRef.update({ paymentStatus: paymentStatus });
            console.log(`[VNPAY IPN] Cap nhat don dat ban ${orderId} thanh ${paymentStatus}`);
        } else if (orderId.startsWith('UPGRADE_')) {
            const parts = orderId.split('_');
            const userId = parts[1];
            const storeId = parts[2];
            const userDoc = await dbFirestore.collection('users').doc(userId).get();
            if (userDoc.exists && userDoc.data()?.role === 'owner') { console.log(`[VNPAY IPN] User ${userId} da la owner. Bo qua.`); return res.status(200).json({ RspCode: '02', Message: 'User already upgraded' }); }
            // Thêm kiểm tra số tiền nếu cần
            if (rspCode == '00') {
                console.log(`[VNPAY IPN] Nhan thanh toan NANG CAP cho user ${userId}`);
                const roleUpdate = { role: 'owner' };
                const rtdbRef = db.ref(`dataUser/${userId}`);
                const firestoreRef = dbFirestore.collection('users').doc(userId);
                const storeRef = db.ref(`dataStoreMain/${storeId}`);
                await Promise.all([ rtdbRef.update(roleUpdate), firestoreRef.update(roleUpdate), storeRef.update({ paymentStatus: 'Đã thanh toán VNPAY' }) ]);
                console.log(`[VNPAY IPN] DA NANG CAP role cho user ${userId} thanh 'owner'`);
            } else {
                console.log(`[VNPAY IPN] Thanh toan NANG CAP cho user ${userId} THAT BAI (RspCode: ${rspCode})`);
                const storeRef = db.ref(`dataStoreMain/${storeId}`);
                await storeRef.update({ paymentStatus: 'FAILED VNPAY' });
            }
        } else {
            console.log(`[VNPAY IPN] Khong nhan ra format cua orderId: ${orderId}`);
            return res.status(200).json({ RspCode: '01', Message: 'Order not found' });
        }
        return res.status(200).json({ RspCode: '00', Message: 'Success' });
    } catch (error) {
        console.error(`[VNPAY IPN] LOI XU LY cho order ${orderId}:`, error);
        return res.status(200).json({ RspCode: '99', Message: 'Internal Server Error' });
    }
});

// ENDPOINT 6: VIETQR IPN URL (Cập nhật DB cho VietQR)
app.post('/vietqr_ipn', async (req, res) => {
    // ... (Code endpoint này giữ nguyên như cũ - NHỚ SỬA THEO API THỰC TẾ) ...
    console.log('[VietQR IPN] Nhận được thông báo:', JSON.stringify(req.body));
    const notificationData = req.body;
    // *** THÊM XÁC THỰC THÔNG BÁO Ở ĐÂY ***
    console.log('[VietQR IPN] Chữ ký hợp lệ (hoặc bỏ qua kiểm tra).');
    // *** SỬA CÁCH LẤY orderId, transactionAmount, transactionStatus ***
    const orderId = notificationData.addInfo || notificationData.description || notificationData.orderId;
    const transactionAmount = notificationData.amount;
    const transactionStatus = notificationData.status || notificationData.transactionStatus;
    if (!orderId) { console.error('[VietQR IPN] Lỗi: Thiếu thông tin mã đơn hàng.'); return res.status(400).send('Missing order identifier'); }
    console.log(`[VietQR IPN] Xu ly cho orderId: ${orderId}, Status: ${transactionStatus}, Amount: ${transactionAmount}`);
    try {
        if (!orderId.startsWith('VIETQR_')) { console.warn(`[VietQR IPN] Bo qua orderId khong phai VietQR dat ban: ${orderId}`); return res.status(200).send('Acknowledged, but not a VietQR booking order.'); }
        const orderRef = db.ref(`dataBookTable/${orderId}`);
        const snapshot = await orderRef.once('value');
        if (!snapshot.exists()) { console.warn(`[VietQR IPN] Don hang ${orderId} khong tim thay.`); return res.status(200).send('Order not found, but acknowledged.'); }
        const orderData = snapshot.val();
        if (!orderData.paymentStatus || !orderData.paymentStatus.startsWith('Chờ thanh toán')) { console.log(`[VietQR IPN] Don hang ${orderId} da duoc xu ly truoc do (${orderData.paymentStatus}). Bo qua.`); return res.status(200).send('Order already processed.'); }
        // *** SỬA CÁC GIÁ TRỊ TRẠNG THÁI THÀNH CÔNG ***
        if (transactionStatus === 'SUCCESS' || transactionStatus === 'PAID' || transactionStatus === '00') {
            // Thêm kiểm tra số tiền nếu cần
            await orderRef.update({ paymentStatus: 'Đã thanh toán (VietQR)' });
            console.log(`[VietQR IPN] Cap nhat don hang ${orderId} thanh cong (VietQR).`);
        } else {
            await orderRef.update({ paymentStatus: 'Thanh toán VietQR thất bại' });
            console.log(`[VietQR IPN] Cap nhat don hang ${orderId} that bai (VietQR - Status: ${transactionStatus}).`);
        }
        res.status(200).send('IPN Processed Successfully');
    } catch (error) {
        console.error(`[VietQR IPN] Loi cap nhat DB cho don hang ${orderId}:`, error);
        res.status(500).send('Internal Database Error');
    }
});

//================================================================================
// TÁC VỤ DỌN DẸP TỰ ĐỘNG (CRON JOB)
//================================================================================

// --- HÀM DỌN DẸP ĐƠN HÀNG ---
const CLEANUP_INTERVAL = 5 * 60 * 1000; // 5 phút
async function cleanupExpiredOrders() {
    // ... (Code hàm này giữ nguyên như cũ) ...
    const now = new Date();
    const ordersRef = db.ref('dataBookTable');
    console.log(`[CRON JOB][Orders] Bat dau chu trinh don dep luc ${now.toLocaleTimeString()}`);
    let deletedCount = 0;
    try { // Chờ thanh toán
        const fiveMinutesAgo = now.getTime() - CLEANUP_INTERVAL;
        const pendingSnapshots = await Promise.all([
             ordersRef.orderByChild('paymentStatus').equalTo('Chờ thanh toán VNPay').once('value'),
             ordersRef.orderByChild('paymentStatus').equalTo('Chờ thanh toán VietQR').once('value')
        ]);
        const promises = [];
        pendingSnapshots.forEach(snapshot => {
            if (snapshot.exists()) {
                snapshot.forEach(childSnapshot => {
                    const order = childSnapshot.val();
                    if (order.createdAt && order.createdAt < fiveMinutesAgo) {
                        console.log(`[CRON JOB][Orders] Don cho thanh toan ${childSnapshot.key} (${order.paymentStatus}) da qua han. Dang xoa...`);
                        promises.push(childSnapshot.ref.remove());
                    }
                });
            }
        });
        if (promises.length > 0) { await Promise.all(promises); deletedCount += promises.length; }
    } catch (error) { console.error('[CRON JOB][Orders] Loi khi don dep don cho thanh toan:', error); }
    try { // Tại quầy
        const cashSnapshot = await ordersRef.orderByChild('status').equalTo('Chờ xử lý').once('value');
        if (cashSnapshot.exists()) {
            const cashPromises = [];
            cashSnapshot.forEach(childSnapshot => {
                const order = childSnapshot.val();
                if (order.dateTime && order.startTime) {
                    try {
                        const [day, month, year] = order.dateTime.split('/');
                        const [hour, minute] = order.startTime.split(':');
                        const bookingStartDateTime = new Date(year, parseInt(month) - 1, day, hour, minute);
                        const deadline = bookingStartDateTime.getTime() + (15 * 60 * 1000);
                        if (now.getTime() > deadline) {
                            console.log(`[CRON JOB][Orders] Don tai quay ${childSnapshot.key} da qua gio hen. Dang xoa...`);
                            cashPromises.push(childSnapshot.ref.remove());
                        }
                    } catch (e) { console.error(`[CRON JOB][Orders] Loi xu ly ngay gio cho don ${childSnapshot.key}:`, e.message); /* cashPromises.push(childSnapshot.ref.remove()); */ }
                } else { console.warn(`[CRON JOB][Orders] Don tai quay ${childSnapshot.key} thieu ngay/gio. Dang xoa...`); cashPromises.push(childSnapshot.ref.remove()); }
            });
            if (cashPromises.length > 0) { await Promise.all(cashPromises); deletedCount += cashPromises.length; }
        }
    } catch (error) { console.error('[CRON JOB][Orders] Loi khi don dep don tai quay:', error); }
    console.log(`[CRON JOB][Orders] Hoan tat chu trinh don dep. Da xoa ${deletedCount} don.`);
}

// --- HÀM DỌN DẸP NGƯỜI DÙNG CHƯA XÁC THỰC ---
const UNVERIFIED_USER_CLEANUP_INTERVAL_DAYS = 15;
const MAX_USERS_PER_DELETE_BATCH = 1000;
async function cleanupUnverifiedUsers() {
    // ... (Copy lại toàn bộ hàm cleanupUnverifiedUsers từ tin nhắn trước vào đây) ...
    const now = new Date();
    const fifteenDaysAgoTimestamp = now.getTime() - (UNVERIFIED_USER_CLEANUP_INTERVAL_DAYS * 24 * 60 * 60 * 1000);
    console.log(`[USER CLEANUP] Bat dau quet user chua xac thuc (qua ${UNVERIFIED_USER_CLEANUP_INTERVAL_DAYS} ngay) luc ${now.toLocaleTimeString()}`);
    let usersToDeleteUids = [];
    let checkedUserCount = 0;
    let deletedUserCount = 0;
    let nextPageToken;
    try {
        do {
            const listUsersResult = await admin.auth().listUsers(1000, nextPageToken);
            checkedUserCount += listUsersResult.users.length;
            listUsersResult.users.forEach(userRecord => {
                const creationTime = new Date(userRecord.metadata.creationTime).getTime();
                if (!userRecord.emailVerified && creationTime < fifteenDaysAgoTimestamp) {
                    const isEmailPasswordProvider = userRecord.providerData.some(provider => provider.providerId === 'password');
                    if (isEmailPasswordProvider) {
                         console.log(`[USER CLEANUP] Tim thay user can xoa: ${userRecord.uid} (Email: ${userRecord.email || 'N/A'}, Tao luc: ${userRecord.metadata.creationTime})`);
                         usersToDeleteUids.push(userRecord.uid);
                    }
                }
                if (usersToDeleteUids.length === MAX_USERS_PER_DELETE_BATCH) {
                    console.log(`[USER CLEANUP] Dang xoa lo ${MAX_USERS_PER_DELETE_BATCH} user...`);
                    admin.auth().deleteUsers(usersToDeleteUids); // Không cần await ở đây nếu không cần đợi kết quả ngay
                    deletedUserCount += usersToDeleteUids.length;
                    usersToDeleteUids = [];
                }
            });
            nextPageToken = listUsersResult.pageToken;
        } while (nextPageToken);
        if (usersToDeleteUids.length > 0) {
            console.log(`[USER CLEANUP] Dang xoa lo cuoi cung (${usersToDeleteUids.length} user)...`);
            await admin.auth().deleteUsers(usersToDeleteUids);
            deletedUserCount += usersToDeleteUids.length;
        }
        console.log(`[USER CLEANUP] Hoan tat quet ${checkedUserCount} user. Da xoa ${deletedUserCount} user chua xac thuc.`);
    } catch (error) { console.error('[USER CLEANUP] Loi nghiem trong khi don dep user:', error); }
}


// --- LÊN LỊCH CHẠY ĐỊNH KỲ ---
setInterval(cleanupExpiredOrders, CLEANUP_INTERVAL); // Dọn đơn hàng mỗi 5 phút

const USER_CLEANUP_RUN_INTERVAL = 15 * 24 * 60 * 60 * 1000; // Dọn user mỗi 24 giờ
setInterval(cleanupUnverifiedUsers, USER_CLEANUP_RUN_INTERVAL);


//================================================================================
// KHỞI ĐỘNG SERVER
//================================================================================
app.listen(port, () => {
    console.log(`Server listening on port ${port}`);
    // Chạy dọn dẹp 1 lần ngay khi server khởi động
    cleanupExpiredOrders();
    cleanupUnverifiedUsers(); // Chạy dọn user khi khởi động
});