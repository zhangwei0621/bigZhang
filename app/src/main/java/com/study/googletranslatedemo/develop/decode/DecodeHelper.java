package com.study.googletranslatedemo.develop.decode;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DecodeHelper {
    // result.add();
    public List<String> test() {
        List<String> result = new ArrayList<>();
        result.add(k(new byte[]{Ascii.EM, -62, -119, 58, 89, 50, -106}, new byte[]{90, -115, -59, 117, Ascii.VT, 109, -89, 126}));
        result.add(k(new byte[]{-82, 17, -57, -42, 101, -20, -26}, new byte[]{-19, 94, -117, -103, 55, -77, -44, 48}));
        result.add(k(new byte[]{112, -8, 67, 115, -123, Ascii.NAK, 92}, new byte[]{51, -73, Ascii.SI, 60, -41, 74, 111, 111}));
        result.add(k(new byte[]{-96, 116, -82, 113, -91, Ascii.GS, 67}, new byte[]{-29, 59, -30, 62, -9, 66, 119, 98}));
        result.add(k(new byte[]{-64, 32, 95, -115, -120, -4, 94}, new byte[]{-125, 111, 19, -62, -38, -93, 107, 92}));
        result.add(k(new byte[]{-5, 34, 34, -101, 88, 8, 5}, new byte[]{-72, 109, 110, -44, 10, 87, 51, -61}));
        result.add(k(new byte[]{92, -43, -76, -112, -126, 38, 75}, new byte[]{Ascii.US, -102, -8, -33, -48, 121, 124, 8}));
        result.add(k(new byte[]{Ascii.EM, 93, 62, 60, -95, 83, 117}, new byte[]{90, Ascii.DC2, 114, 115, -13, Ascii.FF, 77, 97}));
        result.add(k(new byte[]{2, -86, -27, -49, 97, -103, 62}, new byte[]{65, -27, -87, Byte.MIN_VALUE, 51, -58, 7, -122}));
        result.add(k(new byte[]{Ascii.EM, 88, 43, 61, 7, -61, -80, 54}, new byte[]{90, Ascii.ETB, 103, 114, 85, -100, -127, 6}));
        result.add(k(new byte[]{-24, -8, -35, 98, 124, 57, 90, -37}, new byte[]{-85, -73, -111, 45, 46, 102, 107, -22}));
        result.add(k(new byte[]{-97, 105, 5, 93, -20, 95, -60, 39}, new byte[]{-36, 38, 73, Ascii.DC2, -66, 0, -11, Ascii.NAK}));
        result.add(k(new byte[]{-81, 44, Ascii.ETB, 86, 54, 55, -73, -109}, new byte[]{-20, 99, 91, Ascii.EM, 100, 104, -122, -96}));
        result.add(k(new byte[]{Ascii.CAN, 53, -9, -101, 122, Ascii.SO, -45, -36}, new byte[]{91, 122, -69, -44, 40, 81, -30, -24}));
        result.add(k(new byte[]{Ascii.DC4, -111, -6, 38, 109, 60, -21, -51}, new byte[]{87, -34, -74, 105, 63, 99, -38, -8}));
        result.add(k(new byte[]{117, Ascii.ETB, 126, -75, 92, -45, -37, 61}, new byte[]{54, 88, 50, -6, Ascii.SO, -116, -22, Ascii.VT}));
        result.add(k(new byte[]{Ascii.ESC, 122, 0, -13, 105, -121, 117, Ascii.DC4}, new byte[]{88, 53, 76, -68, 59, -40, 68, 35}));
        result.add(k(new byte[]{-58, -76, -18, 66, -25, -3, 90, -60}, new byte[]{-123, -5, -94, Ascii.CR, -75, -94, 107, -4}));
        result.add(k(new byte[]{-85, -6, 3, 102, -60, 85, -68, 118}, new byte[]{-24, -75, 79, 41, -106, 10, -115, 79}));
        result.add(k(new byte[]{65, 78, Ascii.EM, Ascii.RS, 120, 92}, new byte[]{Ascii.DC2, 6, 88, 90, 61, Ascii.SO, 50, 103}));

        return result;
    }

    public String k(byte[] bArr, byte[] bArr2) {
        int length = bArr.length;
        int length2 = bArr2.length;
        int i5 = 0;
        int i6 = 0;
        while (i5 < length) {
            if (i6 >= length2) {
                i6 = 0;
            }
            bArr[i5] = (byte) (bArr[i5] ^ bArr2[i6]);
            i5++;
            i6++;
        }
        return new String(bArr, StandardCharsets.UTF_8);
    }

}
