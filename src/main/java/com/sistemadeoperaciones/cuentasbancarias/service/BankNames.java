package com.sistemadeoperaciones.cuentasbancarias.service;

import java.text.Normalizer;
import java.util.*;

/**
 * Identidad de un banco a partir del texto libre de {@code bank_accounts.banco}.
 *
 * <p>Negocio captura el mismo banco de muchas formas: "Banorte" y "BANORTE ", "SCOTIABANK PFAE"
 * y "SCOTIA NOMINA", "Banco del Bajío" y "BAJIO". Todas deben agruparse como un solo banco. La
 * regla: se normaliza el texto (mayúsculas, sin acentos ni signos) y se toma su primera palabra
 * significativa —se omiten "BANCO", "GRUPO", "DE"…—; dos nombres son el mismo banco si una de
 * esas palabras contiene a la otra ("SCOTIA" está en "SCOTIABANK"). Se exigen al menos cuatro
 * letras para que una palabra corta no absorba bancos distintos.
 *
 * <p>El nombre que se muestra del grupo es la palabra más larga del grupo: "SCOTIABANK".
 */
public final class BankNames {
    private static final int MIN_MATCH = 4;
    private static final Set<String> STOPWORDS = Set.of(
            "BANCO", "BANCA", "BANK", "GRUPO", "FINANCIERO", "INSTITUCION", "MULTIPLE",
            "DE", "DEL", "LA", "EL", "LOS", "Y", "SA", "S", "A", "CV", "SAB", "MEXICO");

    private final Map<String, String> displayByToken;

    private BankNames(Map<String, String> displayByToken) { this.displayByToken = displayByToken; }

    /** Texto comparable: mayúsculas, sin acentos, sólo letras, números y espacios simples. */
    public static String normalize(String raw) {
        if (raw == null) return "";
        String text = Normalizer.normalize(raw, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return text.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9 ]", " ").trim().replaceAll("\\s+", " ");
    }

    /** Primera palabra que identifica al banco, o el texto completo si no hay ninguna. */
    public static String token(String raw) {
        String normalized = normalize(raw);
        for (String word : normalized.split(" ")) {
            if (!word.isEmpty() && !STOPWORDS.contains(word)) return word;
        }
        return normalized;
    }

    private static boolean sameBank(String a, String b) {
        if (a.equals(b)) return true;
        String shorter = a.length() <= b.length() ? a : b;
        String longer = shorter == a ? b : a;
        return shorter.length() >= MIN_MATCH && longer.contains(shorter);
    }

    /**
     * Agrupa los nombres conocidos —normalmente todos los de {@code bank_accounts}— para que
     * {@link #display(String)} resuelva cualquier otro texto contra ese mismo catálogo.
     */
    public static BankNames of(Collection<String> names) {
        List<String> tokens = names.stream().filter(Objects::nonNull).map(BankNames::token)
                .filter(t -> !t.isEmpty()).distinct()
                .sorted(Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder()))
                .toList();
        List<List<String>> groups = new ArrayList<>();
        for (String token : tokens) {
            List<String> target = groups.stream()
                    .filter(group -> group.stream().anyMatch(member -> sameBank(member, token)))
                    .findFirst().orElse(null);
            if (target == null) { target = new ArrayList<>(); groups.add(target); }
            target.add(token);
        }
        Map<String, String> display = new HashMap<>();
        for (List<String> group : groups) {
            String name = group.stream().max(Comparator.comparingInt(String::length)
                    .thenComparing(Comparator.reverseOrder())).orElseThrow();
            group.forEach(member -> display.put(member, name));
        }
        return new BankNames(display);
    }

    /** Nombre del banco al que pertenece el texto; si no está en el catálogo, su propia palabra. */
    public String display(String raw) {
        String token = token(raw);
        if (token.isEmpty()) return "";
        String direct = displayByToken.get(token);
        if (direct != null) return direct;
        return displayByToken.entrySet().stream()
                .filter(entry -> sameBank(entry.getKey(), token))
                .map(Map.Entry::getValue).findFirst().orElse(token);
    }
}
