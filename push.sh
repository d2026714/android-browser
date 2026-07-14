#!/bin/bash
cd /home/work/.openclaw/workspace/android-browser
echo "请输入你的 GitHub Token:"
read -s TOKEN
git remote set-url origin "https://${TOKEN}@github.com/d2026714/android-browser.git"
git add -A
git commit -m "fix: valid GeckoView version + proguard rules" 2>/dev/null
git push origin main
echo ""
echo "✅ 推送完成！建议立即去 https://github.com/settings/tokens 撤销此 token。"
