const nodemailer = require('nodemailer');
const path = require('path');
const ejs = require('ejs');
const { promisify } = require('util');
const fs = require('fs');

const readFile = promisify(fs.readFile);

// Create a test account for development
// In production, use real SMTP credentials
const createTransporter = async () => {
  if (process.env.NODE_ENV === 'production') {
    return nodemailer.createTransport({
      host: process.env.SMTP_HOST,
      port: process.env.SMTP_PORT,
      secure: process.env.SMTP_SECURE === 'true',
      auth: {
        user: process.env.SMTP_USER,
        pass: process.env.SMTP_PASS
      }
    });
  } else {
    // For development, use ethereal.email
    const testAccount = await nodemailer.createTestAccount();
    return nodemailer.createTransport({
      host: 'smtp.ethereal.email',
      port: 587,
      secure: false,
      auth: {
        user: testAccount.user,
        pass: testAccount.pass
      }
    });
  }
};

const sendEmail = async (to, subject, html) => {
  try {
    const transporter = await createTransporter();
    const info = await transporter.sendMail({
      from: `"Soul App" <${process.env.EMAIL_FROM || 'noreply@yourapp.com'}>`,
      to,
      subject,
      html
    });

    console.log('Message sent: %s', info.messageId);
    
    // Preview only available when using ethereal.email
    if (process.env.NODE_ENV !== 'production') {
      console.log('Preview URL: %s', nodemailer.getTestMessageUrl(info));
    }
    
    return info;
  } catch (error) {
    console.error('Error sending email:', error);
    throw error;
  }
};

const renderTemplate = async (templateName, data) => {
  try {
    const templatePath = path.join(__dirname, '../views/emails', `${templateName}.ejs`);
    const template = await readFile(templatePath, 'utf-8');
    return ejs.render(template, data);
  } catch (error) {
    console.error('Error rendering email template:', error);
    throw error;
  }
};

const sendVerificationEmail = async (user, token) => {
  const verificationUrl = `${process.env.FRONTEND_URL || 'http://localhost:3000'}/verify-email?token=${token}`;
  
  const html = await renderTemplate('verify-email', {
    name: user.display_name || user.username,
    verificationUrl,
    appName: 'Soul App',
    supportEmail: 'support@yourapp.com'
  });

  return sendEmail(
    user.email,
    'Verify Your Email Address',
    html
  );
};

const sendPasswordResetEmail = async (user, token) => {
  const resetUrl = `${process.env.FRONTEND_URL || 'http://localhost:3000'}/reset-password?token=${token}`;
  
  const html = await renderTemplate('reset-password', {
    name: user.display_name || user.username,
    resetUrl,
    appName: 'Soul App',
    supportEmail: 'support@yourapp.com',
    expiresIn: '1 hour'
  });

  return sendEmail(
    user.email,
    'Password Reset Request',
    html
  );
};

module.exports = {
  sendEmail,
  sendVerificationEmail,
  sendPasswordResetEmail,
  createTransporter
};
