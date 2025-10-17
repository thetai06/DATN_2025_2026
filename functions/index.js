const express = require('express');
const moment = require('moment');
const crypto = require('crypto');
const querystring = require('qs');
const admin = require('firebase-admin');

const app = express();
const port = process.env.PORT || 3000;

app.use(express.json());

// --- KHỞI TẠO FIREBASE ADMIN SDK ---
const serviceAccount = JSON.parse(process.env.FIREBASE_CREDENTIALS);
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: process.env.FIREBASE_DATABASE_URL
});
const db = admin.database();

// --- CẤU HÌNH ---
const tmnCode = process.env.VNP_TMNCODE;
const secretKey = process.env.VNP_HASHSECRET;
let vnpUrl = process.env.VNP_URL || 'https://sandbox.vnpayment.vn/paymentv2/vpcpay.html';
const frontendSuccessUrl = process.env.FRONTEND_SUCCESS_URL; // Lấy từ biến môi trường
const frontendFailUrl = process.env.FRONTEND_FAIL_URL;       // Lấy từ biến môi trường

// Endpoint để tạo URL thanh toán
app.post('/create_payment_url', (req, res) => {
    const returnUrl = process.env.VNP_RETURNURL;
    const date = new Date();
    const createDate = moment(date).format('YYYYMMDDHHmmss');
    const ipAddr = req.headers['x-forwarded-for'] || req.connection.remoteAddress;
    const { amount, bankCode, language, orderId } = req.body;
    const orderInfo = `Thanh toan don hang ${orderId}`;
    const orderType = 'other';
    const locale = language || 'vn';
    const currCode = 'VND';

    let vnp_Params = {
        'vnp_Version': '2.1.0',
        'vnp_Command': 'pay',
        'vnp_TmnCode': tmnCode,
        'vnp_Locale': locale,
        'vnp_CurrCode': currCode,
        'vnp_TxnRef': orderId,
        'vnp_OrderInfo': orderInfo,
        'vnp_OrderType': orderType,
        'vnp_Amount': amount * 100,
        'vnp_ReturnUrl': returnUrl,
        'vnp_IpAddr': ipAddr,
        'vnp_CreateDate': createDate
    };
    if (bankCode) {
        vnp_Params['vnp_BankCode'] = bankCode;
    }

    vnp_Params = sortObject(vnp_Params);
    const signData = querystring.stringify(vnp_Params, { encode: false });
    const hmac = crypto.createHmac("sha512", secretKey);
    const signed = hmac.update(Buffer.from(signData, 'utf-8')).digest("hex");
    vnp_Params['vnp_SecureHash'] = signed;

    const paymentUrl = vnpUrl + '?' + querystring.stringify(vnp_Params, { encode: false });
    res.json({ paymentUrl });
});

// Endpoint để VNPAY gọi lại sau khi người dùng thanh toán (Return URL)
app.get('/vnpay_return', function (req, res, next) {
    let vnp_Params = req.query;
    const secureHash = vnp_Params['vnp_SecureHash'];
    delete vnp_Params['vnp_SecureHash'];
    delete vnp_Params['vnp_SecureHashType'];

    vnp_Params = sortObject(vnp_Params);
    const signData = querystring.stringify(vnp_Params, { encode: false });
    const hmac = crypto.createHmac("sha512", secretKey);
    const signed = hmac.update(Buffer.from(signData, 'utf-8')).digest("hex");

    if (secureHash === signed) {
        if (vnp_Params['vnp_ResponseCode'] == '00') {
            res.redirect(frontendSuccessUrl);
        } else {
            res.redirect(frontendFailUrl);
        }
    } else {
        res.redirect(frontendFailUrl);
    }
});

// Endpoint để VNPAY gọi lại cập nhật trạng thái (IPN URL)
app.get('/vnpay_ipn', function (req, res, next) {
    let vnp_Params = req.query;
    let secureHash = vnp_Params['vnp_SecureHash'];
    delete vnp_Params['vnp_SecureHash'];
    delete vnp_Params['vnp_SecureHashType'];

    vnp_Params = sortObject(vnp_Params);
    let signData = querystring.stringify(vnp_Params, { encode: false });
    let hmac = crypto.createHmac("sha512", secretKey);
    let signed = hmac.update(Buffer.from(signData, 'utf-8')).digest("hex");

    if (secureHash === signed) {
        const orderId = vnp_Params['vnp_TxnRef'];
        const rspCode = vnp_Params['vnp_ResponseCode'];
        const orderRef = db.ref(`dataBookTable/${orderId}`); // SỬA LẠI: đường dẫn phải khớp với Android

        if (rspCode == '00') {
            // Giao dịch thành công
            orderRef.update({ paymentStatus: 'Đã thanh toán' })
                .then(() => res.status(200).json({ RspCode: '00', Message: 'Success' }))
                .catch(() => res.status(200).json({ RspCode: '99', Message: 'Update DB Error' }));
        } else {
            // Giao dịch thất bại
            orderRef.update({ paymentStatus: 'FAILED' })
                .then(() => res.status(200).json({ RspCode: '00', Message: 'Success' }))
                .catch(() => res.status(200).json({ RspCode: '99', Message: 'Update DB Error' }));
        }
    } else {
        res.status(200).json({ RspCode: '97', Message: 'Invalid Checksum' });
    }
});

// Hàm sắp xếp các thuộc tính của object theo alphabet
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

// ===================================================================
// == BẮT ĐẦU TÁC VỤ DỌN DẸP TỰ ĐỘNG ==
// ===================================================================
const CLEANUP_INTERVAL = 5 * 60 * 1000; // 5 phút

async function cleanupExpiredOrders() {
    const now = new Date(); // Sử dụng đối tượng Date để dễ so sánh
    const ordersRef = db.ref('dataBookTable');
    console.log(`[CRON JOB] Bắt đầu chu trình dọn dẹp lúc ${now.toLocaleTimeString()}`);

    // --- NHIỆM VỤ 1: Quét đơn hàng "Chờ thanh toán VNPay" quá 5 phút ---
    try {
        const fiveMinutesAgo = now.getTime() - CLEANUP_INTERVAL;
        const vnpaySnapshot = await ordersRef.orderByChild('paymentStatus').equalTo('Chờ thanh toán VNPay').once('value');
        if (vnpaySnapshot.exists()) {
            const vnpayPromises = [];
            vnpaySnapshot.forEach(childSnapshot => {
                const order = childSnapshot.val();
                if (order.createdAt && order.createdAt < fiveMinutesAgo) {
                    console.log(`[CRON JOB] Đơn VNPay ${childSnapshot.key} đã quá hạn. Đang xóa...`);
                    vnpayPromises.push(childSnapshot.ref.remove());
                }
            });
            if (vnpayPromises.length > 0) await Promise.all(vnpayPromises);
        }
    } catch (error) {
        console.error('[CRON JOB] Lỗi khi dọn dẹp đơn VNPay:', error);
    }

    // --- NHIỆM VỤ 2: Quét đơn "Thanh toán tại quầy" quá giờ hẹn 15 phút ---
    try {
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
                            console.log(`[CRON JOB] Đơn tại quầy ${childSnapshot.key} đã quá giờ hẹn. Đang xóa...`);
                            cashPromises.push(childSnapshot.ref.remove());
                        }
                    } catch (e) {
                        console.error(`[CRON JOB] Lỗi xử lý ngày giờ cho đơn ${childSnapshot.key}:`, e.message);
                    }
                }
            });
            if (cashPromises.length > 0) await Promise.all(cashPromises);
        }
    } catch (error) {
        console.error('[CRON JOB] Lỗi khi dọn dẹp đơn tại quầy:', error);
    }
    console.log('[CRON JOB] Hoàn tất chu trình dọn dẹp.');
}

// Chạy hàm dọn dẹp mỗi 5 phút
setInterval(cleanupExpiredOrders, CLEANUP_INTERVAL);
// ===================================================================
// == KẾT THÚC TÁC VỤ DỌN DẸP TỰ ĐỘNG ==
// ===================================================================

app.listen(port, () => {
    console.log(`Server listening at http://localhost:${port}`);
});