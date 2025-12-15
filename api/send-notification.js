const nodemailer = require('nodemailer');
const PDFDocument = require('pdfkit');

/**
 * Helper to create a PDF buffer summarizing the event.
 * @param {Object} params
 * @param {string} params.title
 * @param {string} params.message
 * @param {Record<string, any>} [params.details]
 * @returns {Promise<Buffer>}
 */
function createNotificationPdf({ title, message, details }) {
  return new Promise((resolve, reject) => {
    const doc = new PDFDocument();
    const chunks = [];

    doc.on('data', (chunk) => chunks.push(chunk));
    doc.on('end', () => resolve(Buffer.concat(chunks)));
    doc.on('error', (err) => reject(err));

    doc.fontSize(18).text(title, { underline: true });
    doc.moveDown();
    doc.fontSize(12).text(message);
    doc.moveDown();

    if (details && typeof details === 'object') {
      doc.fontSize(12).text('Details:', { underline: true });
      doc.moveDown(0.5);
      Object.entries(details).forEach(([key, value]) => {
        doc.text(`${key}: ${String(value)}`);
      });
    }

    doc.end();
  });
}

/**
 * Vercel serverless function handler.
 * Expects POST with JSON body:
 * {
 *   "toEmail": "user@example.com",
 *   "subject": "Loan Approved",
 *   "title": "Loan Approved",
 *   "message": "Your loan is approved",
 *   "details": { "LoanType": "Gold", "Amount": 50000 }
 * }
 */
module.exports = async (req, res) => {
  if (req.method !== 'POST') {
    res.setHeader('Allow', 'POST');
    return res.status(405).json({ error: 'Method Not Allowed' });
  }

  const { toEmail, subject, title, message, details } = req.body || {};

  if (!toEmail || !subject || !title || !message) {
    return res.status(400).json({
      error: 'Missing required fields: toEmail, subject, title, message'
    });
  }

  // Configure transporter using Gmail or any SMTP provider.
  // Set env vars in Vercel dashboard: MAIL_USER, MAIL_PASS
  const transporter = nodemailer.createTransport({
    service: process.env.MAIL_SERVICE || 'gmail',
    auth: {
      user: process.env.MAIL_USER,
      pass: process.env.MAIL_PASS
    }
  });

  try {
    const pdfBuffer = await createNotificationPdf({ title, message, details });

    await transporter.sendMail({
      from: `"NeoBank" <${process.env.MAIL_FROM || process.env.MAIL_USER}>`,
      to: toEmail,
      subject,
      text: message,
      attachments: [
        {
          filename: 'notification.pdf',
          content: pdfBuffer
        }
      ]
    });

    return res.status(200).json({ success: true });
  } catch (error) {
    // Avoid leaking sensitive error details
    console.error('Error sending notification email:', error);
    return res.status(500).json({ error: 'Failed to send email' });
  }
};




