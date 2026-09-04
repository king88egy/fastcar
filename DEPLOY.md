# Fast Car Racing - Deployment Guide

## şerefe (مجاني 24/7) - Railway

### الخطوة 1: رفع الكود على GitHub
```bash
cd "Fast car"
git init
git add .
git commit -m "FastCar initial"
git remote add origin https://github.com/YOUR_USERNAME/fastcar.git
git push -u origin main
```

### الخطوة 2: إنشاء مشروع على Railway
1. افتح https://railway.app
2. Sign in بحساب GitHub
3. New Project → Deploy from GitHub Repo
4. اختار الريبو بتاعك
5. Railway هيعمل Build تلقائياً من Dockerfile
6. بعد ما يخلص Build، هيعطيك رابط زي `https://fastcar.up.railway.app`

### الخطوة 3: ضبط SMTP (مهم لإرسال الإيميلات)
في Railway → Variables:
```
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=your-email@gmail.com
SMTP_PASS=xxxxxxxx
```

#### Gmail App Password:
1. افتح https://myaccount.google.com/apppasswords
2. سجل دخول بحسابك
3. أنشئ App Password جديد لـ "Fast Car"
4. انسخ الباسورد وحطه في SMTP_PASS

### الخطوة 4: ضبط الرابط في اللعبة
غيّر في:
- `android/src/com/fastcar/racing/Api.java`: `DEFAULT_SERVER_URL`
- `desktop/src/com/fasTcar/desktop/Api.java`: `urlBase`

القيمة الجديدة: `https://fastcar.up.railway.app`

---

## Render (مجاني)
1. ارفع على GitHub
2. https://render.com → New Web Service
3. Connect GitHub repo
4. Build: `docker build -t fastcar .`
5. Start: `java -cp FastCarServer.jar com.fastcar.server.ServerMain`
6. اضبط SMTP variables
7. رابط زي `https://fastcar.onrender.com`

#### Anti-Sleep:
- https://cron-job.org → New Job
- Schedule: Every 10 minutes
- URL: https://fastcar.onrender.com/health

---

## Fly.io (مجاني)
```bash
curl -L https://fly.io/install.sh | sh
fly auth login
fly launch
fly deploy
fly secrets set SMTP_HOST=smtp.gmail.com SMTP_PORT=587 SMTP_USER=you@gmail.com SMTP_PASS=your-app-password
fly open
```

---

## تشغيل محلي
```bash
docker build -t fastcar .
docker run -p 8080:8080 -v fastcar-data:/app/data fastcar
```

---

## بعد النشر - ضبط الرابط التلقائي
غيّر `DEFAULT_SERVER_URL` في Api.java:
```java
public static final String DEFAULT_SERVER_URL = "https://fastcar.up.railway.app";
```

etc.
