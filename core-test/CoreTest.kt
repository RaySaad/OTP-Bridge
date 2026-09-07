import com.masaryamamah.otpbridge.OtpCore

fun main() {
    check(OtpCore.extractOtp("Your OTP is 483921") == "483921")
    check(OtpCore.extractOtp("Ref 1234567890 code 4421") == "4421")
    check(OtpCore.extractOtp("No code") == null)
    check(OtpCore.extractOtp("A 1234 B 567890") == "567890")
    check(OtpCore.isSenderAllowed(" muqeem ", "MUQEEM\nQIWA"))
    check(!OtpCore.isSenderAllowed("BANK", "MUQEEM\nQIWA"))
    check(OtpCore.parseAllowlist("MUQEEM, QIWA;EFAA\nABSher").size == 4)
    check(OtpCore.jsonEscape("a\"b\\c\n") == "a\\\"b\\\\c\\n")
    println("OtpCore tests passed")
}
