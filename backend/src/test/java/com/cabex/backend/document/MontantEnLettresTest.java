package com.cabex.backend.document;

import com.cabex.backend.document.service.MontantEnLettres;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MontantEnLettresTest {

    @Test
    void casSimples() {
        assertEquals("zéro", MontantEnLettres.convertir(0));
        assertEquals("un", MontantEnLettres.convertir(1));
        assertEquals("dix-sept", MontantEnLettres.convertir(17));
        assertEquals("vingt et un", MontantEnLettres.convertir(21));
        assertEquals("soixante et onze", MontantEnLettres.convertir(71));
        assertEquals("quatre-vingts", MontantEnLettres.convertir(80));
        assertEquals("quatre-vingt-un", MontantEnLettres.convertir(81));
        assertEquals("quatre-vingt-onze", MontantEnLettres.convertir(91));
        assertEquals("cent", MontantEnLettres.convertir(100));
        assertEquals("deux cents", MontantEnLettres.convertir(200));
        assertEquals("deux cent un", MontantEnLettres.convertir(201));
        assertEquals("mille", MontantEnLettres.convertir(1000));
        assertEquals("deux mille", MontantEnLettres.convertir(2000));
    }

    @Test
    void millions() {
        assertEquals("un million", MontantEnLettres.convertir(1_000_000));
        assertEquals("vingt et un millions", MontantEnLettres.convertir(21_000_000));
        assertEquals("deux cent douze millions trois cent quarante-cinq mille six cent soixante-dix-huit",
                MontantEnLettres.convertir(212_345_678));
    }
}
