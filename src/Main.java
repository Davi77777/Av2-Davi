
import java.util.*;

public class Main {
    public static void main(String[] args) {
        Grammar glc = new Grammar(
                new HashSet<>(Arrays.asList("S", "A")),
                new HashSet<>(Arrays.asList("a", "b")),
                "S",
                new ArrayList<>(Arrays.asList(
                        new Production("S", Arrays.asList("a", "A")),
                        new Production("A", Arrays.asList("S")),
                        new Production("A", Arrays.asList("b"))
                ))
        );
        Grammar fng = transformToFNG(glc);

        // Imprime a Gramática na Forma Normal de Greibach (FNG)
        System.out.println("Gramática na Forma Normal de Greibach (FNG):");
        for (Production production : fng.productions) {
            System.out.println(production.left + " -> " + String.join(" ", production.right));
        }
    }

    public static Grammar transformToFNG(Grammar glc) {
        Grammar fng = new Grammar(glc.nonTerminals, glc.terminals, glc.startSymbol, glc.productions);

        eliminateEpsilonProductions(glc, fng);
        eliminateUnitProductions(fng);
        eliminateNonTerminalTransitions(fng);
        convertToGreibachNormalForm(fng);

        return fng;
    }

    // Etapa 1: Eliminação das produções vazias
    private static void eliminateEpsilonProductions(Grammar glc, Grammar fng) {
        Set<String> nullableNonTerminals = new HashSet<>();
        int oldNullableCount;
        int newNullableCount;

        // Passo 1: Encontra os não-terminais que são anuláveis
        do {
            oldNullableCount = nullableNonTerminals.size();
            for (Production production : glc.productions) {
                if (production.right.contains(Grammar.EPSILON)) {
                    nullableNonTerminals.add(production.left);
                }
            }

            for (Production production : glc.productions) {
                if (production.right.stream().allMatch(nullableNonTerminals::contains)) {
                    nullableNonTerminals.add(production.left);
                }
            }

            newNullableCount = nullableNonTerminals.size();
        } while (oldNullableCount != newNullableCount);

        // Passo 2: Elimina as produções vazias
        for (Production production : glc.productions) {
            if (production.right.contains(Grammar.EPSILON)) {
                List<List<String>> newProductions = new ArrayList<>();
                List<Production> nonNullableProductions = new ArrayList<>();
                for (Production nonNullableProduction : glc.productions) {
                    if (!nonNullableProduction.equals(production)) {
                        nonNullableProductions.add(nonNullableProduction);
                    }
                }

                newProductions.add(new ArrayList<>(Arrays.asList(production.left)));
                for (String nullableNonTerminal : nullableNonTerminals) {
                    for (Production nonNullableProduction : nonNullableProductions) {
                        List<String> right = new ArrayList<>();
                        for (String symbol : nonNullableProduction.right) {
                            if (symbol.equals(nullableNonTerminal)) {
                                right.add(Grammar.EPSILON);
                            } else {
                                right.add(symbol);
                            }
                        }
                        List<String> newProduction = new ArrayList<>(Arrays.asList(production.left));
                        newProduction.addAll(right);
                        newProductions.add(newProduction);
                    }
                }

                for (List<String> newProduction : newProductions) {
                    fng.productions.add(new Production(newProduction.get(0), new
                            Production.subList(1, newProduction.size())));
                }
            }
        }
    }

    // Etapa 2: Eliminação das produções unitárias
    public static void eliminateUnitProductions(Grammar g) {
        List<Production> unitProductions = new ArrayList<>();
        for (Production production : g.productions) {
            if (production.right.size() == 1 && g.nonTerminals.contains(production.right.get(0))) {
                unitProductions.add(production);
            }
        }

        // Passo 3: Elimina as produções unitárias
        for (Production unitProduction : unitProductions) {
            List<Production> nonUnitProductions = new ArrayList<>();
            for (Production production : g.productions) {
                if (!production.equals(unitProduction)) {
                    nonUnitProductions.add(production);
                }
            }
            Set<String> visited = new HashSet<>();
            Deque<String> queue = new LinkedList<>();
            queue.add(unitProduction.right.get(0));

            while (!queue.isEmpty()) {
                String current = queue.removeFirst();
                if (!visited.contains(current)) {
                    visited.add(current);
                    for (Production nonUnitProduction : nonUnitProductions) {
                        if (nonUnitProduction.left.equals(current)) {
                            queue.addAll(nonUnitProduction.right);
                            g.productions.add(new Production(unitProduction.left, nonUnitProduction.right));
                        }
                    }
                }
            }
        }
        g.productions.removeAll(unitProductions);
    }

    // Etapa 3: Eliminação das transições entre não-terminais
    public static void eliminateNonTerminalTransitions(Grammar g) {
        List<Production> nonTerminalTransitions = new ArrayList<>();
        for (Production production : g.productions) {
            if (production.right.stream().allMatch(g.nonTerminals::contains)) {
                nonTerminalTransitions.add(production);
            }
        }

        // Passo 4: Elimina as transições entre não-terminais
        for (Production nonTerminalTransition : nonTerminalTransitions) {
            Set<String> visited = new HashSet<>();
            Deque<String> queue = new LinkedList<>(nonTerminalTransition.right);

            while (!queue.isEmpty()) {
                String current = queue.removeFirst();
                if (!visited.contains(current)) {
                    visited.add(current);
                    for (Production nonTerminalProduction : g.productions) {
                        if (nonTerminalProduction.left.equals(current)) {
                            queue.addAll(nonTerminalProduction.right);
                            g.productions.add(new Production(nonTerminalTransition.left, nonTerminalProduction.right));
                        }
                    }
                }
            }
        }
        g.productions.removeAll(nonTerminalTransitions);
    }

    // Etapa 4: Conversão para a forma normal de Greibach
    public static void convertToGreibachNormalForm(Grammar g) {
        Set<String> newNonTerminals = new HashSet<>();
        Set<String> newTerminals = new HashSet<>();
        int productionCount = 1;

        // Passo 5: Converte para a forma normal de Greibach
        List<Production> newProductions = new ArrayList<>();
        for (Production production : g.productions) {
            List<String> right = new ArrayList<>();

            for (String symbol : production.right) {
                if (g.terminals.contains(symbol)) {
                    newTerminals.add(symbol);
                    right.add(symbol);
                } else if (g.nonTerminals.contains(symbol)) {
                    newNonTerminals.add(symbol);
                    right.add(symbol);
                } else {
                    String newNonTerminal = "N" + productionCount++;
                    newNonTerminals.add(newNonTerminal);
                    right.add(new NonTerminal);
                    newProductions.add(new Production(newNonTerminal, Arrays.asList(symbol)));
                }
            }

            if (!right.isEmpty()) {
                newProductions.add(new Production(production.left, right));
            }
        }

        g.nonTerminals = newNonTerminals;
        g.terminals = newTerminals;
        g.productions = newProductions;
    }

    // Classe que representa uma gramática
    static class Grammar {
        Set<String> nonTerminals;
        Set<String> terminals;
        String startSymbol;
        List<Production> productions;

        Grammar(Set<String> nonTerminals, Set<String> terminals, String startSymbol, List<Production> productions) {
            this.nonTerminals = nonTerminals;
            this.terminals = terminals;
            this.startSymbol = startSymbol;
            this.productions = productions;
        }

        static final String EPSILON = "ε";
    }

    // Classe que representa uma regra de produção
    static class Production {
        String left;
        List<String> right;

        Production(String left, List<String> right) {
            this.left = left;
            this.right = right;
        }
    }
}
