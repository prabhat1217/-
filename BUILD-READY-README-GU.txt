PROTOCOL SAKHA v13 - FIXED BUILD (APK + EXE)

આ ઝિપમાં શું સુધાર્યું (તપાસમાં મળેલી ખામીઓ):
1. ANDROID: JS alert/confirm/prompt WebView માં બતાવાતા ન હતા -> ડિલીટ/કન્ફર્મ બટન કામ ન કરતા. હવે ગુજરાતી ઓકે/રદ ડાયલોગ.
2. ANDROID: ફાઇલ પસંદગી (બેકઅપ રિસ્ટોર, ફોટો) ચાલતી ન હતી -> ઉમેર્યું.
3. ANDROID: PDF/બેકઅપ સેવ-શેર, Back બટન, ઓટો-બેકઅપ (Documents/PROTOCOL-SAKHA) માટે નેટિવ ભાગ ન હતો -> ઉમેર્યો.
4. ANDROID: એપનું નામ "Protocol Police Duty" હતું -> "PROTOCOL SAKHA". INTERNET permission, WhatsApp/ફોન લિંક, રોટેશન/કીબોર્ડમાં પેજ રીલોડ ન થાય તે સુધાર્યું.
5. ANDROID: APK બનાવવાનો workflow ન હતો -> build-android.yml ઉમેર્યો. APK હંમેશા એક જ key થી સાઇન થાય છે, એટલે નવી APK જૂની ઉપર install થશે અને ડેટા રહેશે.
6. WINDOWS: prompt() Electron માં ચાલતું નથી -> નાની ડાયલોગ વિન્ડો. ઓટો-સેવ ડેટા Documents\PROTOCOL-SAKHA માં (ઉમેર્યું).
7. બંને: PDF/ફોટો લાઇબ્રેરી (jsPDF, html2canvas) બિલ્ડ વખતે અંદર જ મૂકાય છે, એટલે offline પણ PDF બને.
8. WINDOWS: WhatsApp/લિંક બહારના બ્રાઉઝરમાં ખુલે, એક જ વાર એપ ખુલે, પેજ crash થાય તો ફરી લોડ.

બિલ્ડ (GitHub માં બધી ફાઇલો/ફોલ્ડર root માં અપલોડ કરો, .github ફોલ્ડર સાથે):
- Actions -> "Build PROTOCOL SAKHA Android APK" -> Run workflow -> artifact "PROTOCOL-SAKHA-Android-APK"
- Actions -> "Build PROTOCOL SAKHA Windows EXE" -> Run workflow -> artifact "PROTOCOL-SAKHA-Windows-v13"
  (Setup.exe અને Portable.exe બંને મળશે)

મહત્વ: જો ફોનમાં પહેલાની APK બીજી key થી બનેલી હોય તો પહેલી વાર "App not installed" આવશે.
તે વખતે જૂની એપમાં પહેલા બેકઅપ ફાઇલ સેવ કરો, પછી અનઇન્સ્ટોલ કરી નવી install કરો અને બેકઅપ રિસ્ટોર કરો.
પછીની બધી APK સીધી અપડેટ થશે.
