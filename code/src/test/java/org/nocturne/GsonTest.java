package org.nocturne;

import com.google.gson.Gson;
import junit.framework.TestCase;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 27.01.12
 */
public class GsonTest extends TestCase {
    public void testGson() throws Exception {
        Gson gson = new Gson();

        String valueA = "Can't release contest, because there are validation errors:<br/>"
                + "On testset 'tests' SAS detected 'stdin' as problem 'A' input file name, but specified 'asteroids.in'.<br/>"
                + "On testset 'tests' SAS detected 'stdout' as problem 'A' output file name, but specified 'asteroids.out'.<br/>"
                + "On testset 'tests' SAS detected some errors in problem 'A': {{asteroids_rs.java: Can't compile solution file}, {asteroids_petr.java: Can't compile solution file}, {check.dpr: Can't compile checker file}, {asteroids_as.java: Can't compile solution file}, {asteroids_mb.java: Can't compile solution file}, {asteroids_re.java: Can't compile solution file}, {asteroids_gk.java: Can't compile solution file}}.<br/>"
                + "On testset 'tests' SAS detected 'stdin' as problem 'B' input file name, but specified 'business.in'.<br/>"
                + "On testset 'tests' SAS detected 'stdout' as problem 'B' output file name, but specified 'business.out'.<br/>"
                + "On testset 'tests' SAS detected some errors in problem 'B': {{business_rs_wa.dpr: Can't compile solution file}, {business_re.java: Can't compile solution file}, {business_gk_tl.java: Can't compile solution file}, {business_gk.java: Can't compile solution file}, {business_gk_wa.java: Can't compile solution file}, {business_ia_vd.java: Can't compile solution file}, {business_mb.java: Can't compile solution file}, {business_petr.java: Can't compile solution file}, {check.dpr: Can't compile checker file}, {business_rs.dpr: Can't compile solution file}}.<br/>"
                + "On testset 'tests' SAS detected some errors in problem 'C': {{check.dpr: Can't compile checker file}}.<br/>"
                + "On testset 'tests' SAS detected 'stdin' as problem 'D' input file name, but specified 'database.in'.<br/>"
                + "On testset 'tests' SAS detected 'stdout' as problem 'D' output file name, but specified 'database.out'.<br/>"
                + "On testset 'tests' SAS detected some errors in problem 'D': {{database_ia.java: Can't compile solution file}, {check.dpr: Can't compile checker file}, {database_md.java: Can't compile solution file}, {database_re.java: Can't compile solution file}, {database_gk_ok_cpp_full_compact.cpp: Can't compile solution file}, {database_petr.java: Can't compile solution file}, {database_mb.java: Can't compile solution file}, {database_gk_ok_cpp_map_pair.cpp: Can't compile solution file}, {database_gk_ok_cpp_map_nested.cpp: Can't compile solution file}, {database_gk_ok_cpp_map_concat.cpp: Can't compile solution file}, {database_rs.java: Can't compile solution file}}.<br/>"
                + "On testset 'tests' SAS detected 'stdin' as problem 'E' input file name, but specified 'exclusive.in'.<br/>"
                + "On testset 'tests' SAS detected 'stdout' as problem 'E' output file name, but specified 'exclusive.out'.<br/>"
                + "On testset 'tests' SAS detected some errors in problem 'E': {{exclusive_petr.java: Can't compile solution file}, {exclusive_rs.java: Can't compile solution file}, {exclusive_ft.java: Can't compile solution file}, {exclusive_mb.java: Can't compile solution file}, {exclusive_gk.java: Can't compile solution file}, {check.dpr: Can't compile checker file}}.<br/>"
                + "On testset 'tests' SAS detected 'stdin' as problem 'F' input file name, but specified 'funny.in'.<br/>"
                + "On testset 'tests' SAS detected 'stdout' as problem 'F' output file name, but specified 'funny.out'.<br/>"
                + "On testset 'tests' SAS detected some errors in problem 'F': {{funny_gk_wa_skip_words.java: Can't compile solution file}, {funny_ia_wrong.java: Can't compile solution file}, {funny_rs_tl.java: Can't compile solution file}, {check.dpr: Can't compile checker file}, {funny_mb.java: Can't compile solution file}, {funny_ia.java: Can't compile solution file}, {funny_gk.java: Can't compile solution file}, {funny_gk_wa_short_eager.java: Can't compile solution file}, {funny_petr.java: Can't compile solution file}, {funny_gk_wa_no_generated.java: Can't compile solution file}, {funny_gk_wa_short.java: Can't compile solution file}, {funny_rs.java: Can't compile solution file}}.<br/>"
                + "On testset 'tests' SAS detected 'stdin' as problem 'G' input file name, but specified 'garbling.in'.<br/>"
                + "On testset 'tests' SAS detected 'stdout' as problem 'G' output file name, but specified 'garbling.out'.<br/>"
                + "On testset 'tests' SAS detected some errors in problem 'G': {{check.dpr: Can't compile checker file}, {garbling_re.java: Can't compile solution file}, {garbling_rs.java: Can't compile solution file}}.<br/>"
                + "On testset 'tests' SAS detected 'stdin' as problem 'H' input file name, but specified 'headshot.in'.<br/>"
                + "On testset 'tests' SAS detected 'stdout' as problem 'H' output file name, but specified 'headshot.out'.<br/>"
                + "On testset 'tests' SAS detected some errors in problem 'H': {{headshot_gk.java: Can't compile solution file}, {headshot_rs.dpr: Can't compile solution file}, {headshot_petr.java: Can't compile solution file}, {check.dpr: Can't compile checker file}, {headshot_mb.java: Can't compile solution file}, {headshot_ia.java: Can't compile solution file}, {headshot_re.java: Can't compile solution file}}.<br/>"
                + "On testset 'tests' SAS detected 'stdin' as problem 'I' input file name, but specified 'inspection.in'.<br/>"
                + "On testset 'tests' SAS detected 'stdout' as problem 'I' output file name, but specified 'inspection.out'.<br/>"
                + "On testset 'tests' SAS detected some errors in problem 'I': {{inspection_mb.java: Can't compile solution file}, {inspection_rs_wa.java: Can't compile solution file}, {check.dpr: Can't compile checker file}, {inspection_rs.java: Can't compile solution file}, {inspection_petr.java: Can't compile solution file}, {inspection_pm.java: Can't compile solution file}}.<br/>"
                + "On testset 'tests' SAS detected 'stdin' as problem 'J' input file name, but specified 'javacert.in'.<br/>"
                + "On testset 'tests' SAS detected 'stdout' as problem 'J' output file name, but specified 'javacert.out'.<br/>"
                + "On testset 'tests' SAS detected some errors in problem 'J': {{check.dpr: Can't compile checker file}, {javacert_re.java: Can't compile solution file}, {javacert_pm_round.java: Can't compile solution file}, {javacert_petr.java: Can't compile solution file}, {javacert_pm.java: Can't compile solution file}, {javacert_mb.java: Can't compile solution file}, {javacert_re_backtrack.java: Can't compile solution file}, {javacert_rs.java: Can't compile solution file}, {javacert_vd.java: Can't compile solution file}}.<br/>";

        assertEquals(
                "Restored from JSON \'valueA\' does not equal to original.",
                valueA, gson.fromJson(gson.toJson(valueA), String.class)
        );
    }
}
