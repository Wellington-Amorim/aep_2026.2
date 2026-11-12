package br.com.observacao.cli;

import br.com.observacao.model.Atualizacao;
import br.com.observacao.model.Endereco;
import br.com.observacao.model.Solicitacao;
import br.com.observacao.model.Usuario;
import br.com.observacao.service.SolicitacaoService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.Scanner;

@Component
public class TerminalMenu implements CommandLineRunner {

    private final SolicitacaoService solicitacaoService;

    public TerminalMenu(SolicitacaoService solicitacaoService) {
        this.solicitacaoService = solicitacaoService;
    }

    @Override
    public void run(String... args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        int opcao = -1;

        while (opcao != 0) {
            System.out.println("\n=== ObservaAcao - ODS ===");
            System.out.println("1. Registrar nova solicitacao");
            System.out.println("2. Listar solicitacoes");
            System.out.println("3. Atualizar status de solicitacao");
            System.out.println("4. Excluir solicitacao");
            System.out.println("5. Ver detalhes da solicitacao");
            System.out.println("0. Sair");
            System.out.print("Escolha: ");

            opcao = lerInteiro(scanner);

            switch (opcao) {
                case 1 -> registrarSolicitacao(scanner);
                case 2 -> listarSolicitacoes();
                case 3 -> atualizarStatus(scanner);
                case 4 -> excluirSolicitacao(scanner);
                case 5 -> verDetalhesSolicitacao(scanner);
                case 0 -> System.out.println("\nPrograma encerrado.");
                default -> System.out.println("\nOpcao invalida.");
            }
        }

        scanner.close();
    }

    private int lerInteiro(Scanner scanner) {
        while (true) {
            try {
                return Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.print("Entrada invalida. Digite um numero: ");
            }
        }
    }

    private void registrarSolicitacao(Scanner scanner) {
        Solicitacao novaSolic = new Solicitacao();

        System.out.println("\nSelecione a categoria:");
        System.out.println("1. Iluminacao");
        System.out.println("2. Buraco na via");
        System.out.println("3. Limpeza urbana");
        System.out.println("4. Saude");
        System.out.println("5. Seguranca Escolar");
        System.out.println("6. Outros");
        System.out.print("Opcao de categoria: ");

        int opcaoCategoria = lerInteiro(scanner);

        String nomeCategoria = switch (opcaoCategoria) {
            case 1 -> "Iluminacao";
            case 2 -> "Buraco na via";
            case 3 -> "Limpeza urbana";
            case 4 -> "Saude";
            case 5 -> "Seguranca Escolar";
            default -> {
                System.out.print("Digite o nome da categoria (Outros): ");
                yield scanner.nextLine();
            }
        };
        novaSolic.setCategoria(nomeCategoria);

        System.out.println("\nSelecione a prioridade:");
        System.out.println("1. Alta");
        System.out.println("2. Media");
        System.out.println("3. Baixa");
        System.out.print("Opcao de prioridade: ");

        int opcaoPrioridade = lerInteiro(scanner);

        String nomePrioridade = switch (opcaoPrioridade) {
            case 1 -> "Alta";
            case 2 -> "Media";
            case 3 -> "Baixa";
            default -> "Baixa";
        };
        novaSolic.setPrioridade(nomePrioridade);

        System.out.print("Digite a descricao (minimo 20 caracteres): ");
        novaSolic.setDescricao(scanner.nextLine());

        System.out.println("\nInforme o local do problema:");
        Endereco endereco = new Endereco();

        System.out.print("Logradouro (Ex: Rua A, 123): ");
        endereco.setLogradouro(scanner.nextLine());

        System.out.print("Bairro: ");
        endereco.setBairro(scanner.nextLine());

        System.out.print("Ponto de Referencia: ");
        endereco.setPontoReferencia(scanner.nextLine());

        novaSolic.setEndereco(endereco);

        Atualizacao abertura = new Atualizacao(
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                "Solicitacao registrada pelo cidadao",
                "Sistema"
        );
        novaSolic.getHistorico().add(abertura);

        novaSolic.setAnonimo(false);

        System.out.print("\nDeseja registrar de forma anonima? (S/N): ");
        String respostaAnonimo = scanner.nextLine();

        Usuario usuarioInfo = new Usuario();

        if (respostaAnonimo.equalsIgnoreCase("S")) {
            novaSolic.setAnonimo(true);
        } else {
            novaSolic.setAnonimo(false);
            System.out.println("--- Dados do Cidadão ---");
            System.out.print("Nome completo: ");
            usuarioInfo.setNome(scanner.nextLine());
            System.out.print("E-mail: ");
            usuarioInfo.setEmail(scanner.nextLine());
        }

        try {
            solicitacaoService.criarSolicitacao(novaSolic, usuarioInfo);
            System.out.println("\nSolicitacao registrada com sucesso e vinculada ao banco de dados!");
        } catch (Exception e) {
            System.out.println("\nErro ao registrar: " + e.getMessage());
        }
    }

    private void listarSolicitacoes() {
        System.out.println("\n--- Lista de Solicitacoes ---");
        solicitacaoService.listarTodas().forEach(s ->
                System.out.println("Protocolo: " + s.getCodigo() + " | Categoria: " + s.getCategoria() + " | Status: " + s.getStatus())
        );
    }

    private void atualizarStatus(Scanner scanner) {
        System.out.println("\n--- Atualizar Status ---");
        System.out.print("Digite o codigo do protocolo: ");
        String codigoAlt = scanner.nextLine();
        System.out.print("Digite o novo status: ");
        String novoStatus = scanner.nextLine();
        try {
            solicitacaoService.atualizarStatus(codigoAlt, novoStatus);
            System.out.println("\nStatus atualizado com sucesso!");
        } catch (Exception e) {
            System.out.println("\nErro ao atualizar: " + e.getMessage());
        }
    }

    private void excluirSolicitacao(Scanner scanner) {
        System.out.println("\n--- Excluir Solicitacao ---");
        System.out.print("Digite o codigo do protocolo: ");
        String codigoDel = scanner.nextLine();
        try {
            solicitacaoService.excluirPorCodigo(codigoDel);
            System.out.println("\nSolicitacao excluida com sucesso!");
        } catch (Exception e) {
            System.out.println("\nErro ao excluir: " + e.getMessage());
        }
    }

    private void verDetalhesSolicitacao(Scanner scanner) {
        System.out.print("\nDigite o codigo do protocolo (Ex: REQ-2026-ABCD): ");
        String codigo = scanner.nextLine();

        try {
            Solicitacao sol = solicitacaoService.buscarPorCodigo(codigo);

            System.out.println("\n=======================================================");
            System.out.println("DETALHES DA SOLICITAÇÃO: " + sol.getCodigo());
            System.out.println("=======================================================");
            System.out.println("Categoria..: " + sol.getCategoria());
            System.out.println("Prioridade.: " + sol.getPrioridade() + " (SLA: " + sol.getSla() + ")");
            System.out.println("Status.....: " + sol.getStatus());
            System.out.println("Data.......: " + sol.getData());
            System.out.println("Descricao..: " + sol.getDescricao());

            System.out.println("\nDADOS DO CIDADÃO (Coleção Relacionada)");
            if (sol.isAnonimo()) {
                System.out.println("Registro...: Anônimo");
            } else {
                System.out.println("Nome.......: " + sol.getCidadao().getNome());
                System.out.println("E-mail.....: " + sol.getCidadao().getEmail());
            }

            System.out.println("\nLOCALIZAÇÃO (Objeto Complexo)");
            if (sol.getEndereco() != null) {
                System.out.println("Logradouro.: " + sol.getEndereco().getLogradouro());
                System.out.println("Bairro.....: " + sol.getEndereco().getBairro());
                System.out.println("Referência.: " + sol.getEndereco().getPontoReferencia());
            } else {
                System.out.println("Endereço não informado.");
            }

            System.out.println("\nHISTÓRICO DE ATUALIZAÇÕES (Subdocumentos Aninhados)");
            if (sol.getHistorico() != null && !sol.getHistorico().isEmpty()) {
                for (Atualizacao att : sol.getHistorico()) {
                    System.out.println("- [" + att.getDataHora() + "] " + att.getNota() + " (Autor: " + att.getAutor() + ")");
                }
            } else {
                System.out.println("Sem histórico registrado.");
            }
            System.out.println("=======================================================\n");

        } catch (Exception e) {
            System.out.println("\nErro: " + e.getMessage());
        }
    }
}