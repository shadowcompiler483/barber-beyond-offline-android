Barber & Beyond signed-release diagnostic

This ZIP updates only .github/workflows/build-signed-release.yml.
No business records, Android application code, or signing secrets are changed.

On Acode:
1. Extract the ZIP outside the GitHub working folder.
2. Run: find /public -name README-DIAGNOSTIC.txt
3. Identify the directory reported; copy its contents into /public/barber-beyond-repo (preserving hidden .github folder).
4. Run:
    cd /public/barber-beyond-repo
    git add .github/workflows/build-signed-release.yml
    git commit -m "Diagnose Android signing failure safely"
    git push origin main
5. Go to GitHub Actions -> Build signed Barber Beyond release APK -> Run workflow.
6. In the job log look for TEST A and TEST B and report only which one failed, not any key or password.
