import express from "express";
import bcrypt from "bcryptjs";
import jwt from "jsonwebtoken";
import { OAuth2Client } from "google-auth-library";
import { PrismaClient } from "@prisma/client";

const router = express.Router();
const prisma = new PrismaClient();
const googleClient = new OAuth2Client(process.env.GOOGLE_CLIENT_ID);

const JWT_SECRET = process.env.JWT_SECRET || "your-secret-key";
if (JWT_SECRET === "your-secret-key") {
  console.warn("WARNING: JWT_SECRET is using a default value. Please set process.env.JWT_SECRET in your .env file for production.");
}

// Middleware để bảo vệ các route
const protect = (req, res, next) => {
  const token = req.headers.authorization?.split(" ")[1];

  if (!token) {
    return res.status(401).json({ error: "No token provided, authorization denied" });
  }

  try {
    const decoded = jwt.verify(token, JWT_SECRET);
    req.user = decoded; // Gán thông tin người dùng đã giải mã vào req
    next();
  } catch (error) {
    console.error("Token verification error:", error);
    return res.status(401).json({ error: "Token is not valid" });
  }
};



// ============ REGISTER (Email + Password) ============
router.post("/register", async (req, res) => {
  try {
    const { email, username, password } = req.body;

    // Validate
    if (!email || !username || !password) {
      return res.status(400).json({ error: "Missing required fields" });
    }

    // Check if user exists
    const existingUser = await prisma.user.findFirst({
      where: {
        OR: [{ email }, { username }],
      },
    });

    if (existingUser) {
      return res.status(409).json({ error: "Email or username already exists" }); // 409 Conflict
    }

    // Hash password
    const hashedPassword = await bcrypt.hash(password, 10);

    // Create user
    const user = await prisma.user.create({
      data: {
        email,
        username,
        password: hashedPassword,
      },
    });

    // Generate JWT
    const token = jwt.sign({ id: user.id, email: user.email }, JWT_SECRET, {
      expiresIn: "7d",
    });

    return res.status(201).json({
      message: "User registered successfully",
      user: {
        id: user.id,
        email: user.email,
        username: user.username,
      },
      token,
    });
  } catch (error) {
    console.error("Register error:", error.message);
    return res.status(500).json({ error: error.message });
  }
});

// ============ LOGIN (Email + Password) ============
router.post("/login", async (req, res) => {
  try {
    const { email, password } = req.body;

    // Validate
    if (!email || !password) {
      return res.status(400).json({ error: "Email and password required" });
    }

    // Find user
    const user = await prisma.user.findUnique({
      where: { email },
    });

    if (!user || !user.password) {
      return res.status(401).json({ error: "Invalid email or password" });
    }

    // Check password
    const isPasswordValid = await bcrypt.compare(password, user.password);

    if (!isPasswordValid) {
      return res.status(401).json({ error: "Invalid email or password" });
    }

    // Generate JWT
    const token = jwt.sign({ id: user.id, email: user.email }, JWT_SECRET, {
      expiresIn: "7d",
    });

    return res.status(200).json({
      message: "Login successful",
      user: {
        id: user.id,
        email: user.email,
        username: user.username,
        avatar: user.avatar,
      },
      token,
    });
  } catch (error) {
    console.error("Login error:", error.message);
    return res.status(500).json({ error: error.message });
  }
});

// ============ LOGIN WITH GOOGLE ============
router.post("/google", async (req, res) => {
  try {
    const { idToken } = req.body;

    if (!idToken) {
      return res.status(400).json({ error: "ID token required" });
    }

    // Verify Google token
    const ticket = await googleClient.verifyIdToken({
      idToken,
      audience: process.env.GOOGLE_CLIENT_ID,
    });

    const { sub: googleId, email, name, picture } = ticket.getPayload();

    // Check if user exists with this googleId
    let user = await prisma.user.findUnique({ where: { googleId } });

    // If user exists but googleId is null (e.g., registered with email/password first)
    // Or if user exists with email but no googleId
    if (!user && email) {
      user = await prisma.user.findUnique({ where: { email } });
      if (user && !user.googleId) {
        // Link existing account with Google ID
        user = await prisma.user.update({
          where: { id: user.id },
          data: { googleId, googleEmail: email, avatar: picture || user.avatar },
        });
      }
    }

    // If not, create new user
    if (!user) {
      // Generate a unique username if 'name' is not available or might conflict
      let uniqueUsername = name || email.split("@")[0];
      let usernameExists = await prisma.user.findUnique({ where: { username: uniqueUsername } });
      let counter = 1;
      while (usernameExists) {
        uniqueUsername = `${name || email.split("@")[0]}${counter++}`;
        usernameExists = await prisma.user.findUnique({ where: { username: uniqueUsername } });
      }

      user = await prisma.user.create({
        data: {
          googleId,
          email: email, // Use Google email as primary email
          googleEmail: email,
          username: uniqueUsername,
          avatar: picture,
        },
      });
    }

    // Generate JWT
    const token = jwt.sign({ id: user.id, email: user.email }, JWT_SECRET, {
      expiresIn: "7d",
    });

    return res.status(200).json({
      message: "Google login successful",
      user: {
        id: user.id,
        email: user.email,
        username: user.username,
        avatar: user.avatar,
      },
      token,
    });
  } catch (error) {
    console.error("Google login error:", error.message);
    return res.status(401).json({ error: "Invalid Google token" });
  }
});

// ============ GET CURRENT USER (Protected) ============
router.get("/me", protect, async (req, res) => { // Sử dụng middleware 'protect'
  try {
    const user = await prisma.user.findUnique({
      where: { id: req.user.id }, // Thông tin user từ middleware
    });

    if (!user) {
      return res.status(404).json({ error: "User not found" });
    }

    return res.status(200).json({
      user: {
        id: user.id,
        email: user.email,
        username: user.username,
        avatar: user.avatar,
        bio: user.bio,
      },
    });
  } catch (error) {
    console.error("Get user error:", error.message);
    return res.status(500).json({ error: "Failed to retrieve user data" });
  }
});

export default router;
