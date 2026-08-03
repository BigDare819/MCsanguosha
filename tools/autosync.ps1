# 三国杀模组 - 自动同步脚本 (方案2: git 自动 push/pull)
# 用法:
#   .\tools\autosync.ps1                  # 以默认间隔(30s)运行,自动提交+推送
#   .\tools\autosync.ps1 -IntervalSec 60  # 自定义间隔
# 说明:
#   - 持续监控 git status,有改动则 commit + push 到远程
#   - 会忽略 .gitignore 里的内容(build/ .gradle/ runs/ 等)
#   - 推送失败(网络/未配置远程)会静默跳过,不中断
param(
    [string]$RepoPath = "C:\Users\BigMer\NeoForgeMods\examplemod",
    [int]$IntervalSec = 30
)

Set-Location $RepoPath

Write-Host "[autosync] 监控目录: $RepoPath (每 ${IntervalSec}s 检查一次)" -ForegroundColor Cyan
Write-Host "[autosync] Ctrl+C 退出" -ForegroundColor Cyan

while ($true) {
    Start-Sleep -Seconds $IntervalSec

    # 检查是否有未提交改动(含未跟踪文件)
    $changes = git status --porcelain 2>$null
    if (-not $changes) { continue }

    # 等待 5 秒让编辑器写完文件,避免提交半成品
    Start-Sleep -Seconds 5
    $changes2 = git status --porcelain 2>$null
    if (-not $changes2) { continue }

    $ts = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $count = ($changes2 | Measure-Object).Count
    Write-Host "[autosync] $ts 检测到 $count 处改动,自动提交..." -ForegroundColor Yellow

    git add -A 2>$null | Out-Null
    git commit -m "auto-sync: $ts" 2>$null | Out-Null

    # 推送;失败(如未配置远程/断网)则静默跳过,下次再试
    $pushResult = git push 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[autosync] 已推送到远程 ✔" -ForegroundColor Green
    } else {
        Write-Host "[autosync] 推送失败(稍后自动重试): $($pushResult -join ' ')" -ForegroundColor Red
    }
}
