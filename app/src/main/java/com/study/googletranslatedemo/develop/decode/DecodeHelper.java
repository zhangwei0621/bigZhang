package com.study.googletranslatedemo.develop.decode;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DecodeHelper {
    // result.add();
    public List<String> test() {
        List<String> result = new ArrayList<>();
        result.add(k(new byte[]{58, -111, -10, 39, 96, -6, 34, -74, 61, -124, -14, Ascii.NAK, 104, -76, 56, -80, 3, -48, -90, 100, 114, -80, 41, -85}, new byte[]{92, -29, -105, 74, 5, -43, 75, -37}));
        result.add(k(new byte[]{56, -84, -106, -4, -126, 44, -74, -103, 63, -71, -110, -50, -127, 113, -66, -103, 59, -127, -60, -96, -55, 116, -70, -106, 46}, new byte[]{94, -34, -9, -111, -25, 3, -33, -12}));

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
