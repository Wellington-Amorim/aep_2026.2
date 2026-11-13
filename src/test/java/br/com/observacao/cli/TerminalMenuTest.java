package br.com.observacao.cli;

import br.com.observacao.model.Atualizacao;
import br.com.observacao.model.Endereco;
import br.com.observacao.model.Solicitacao;
import br.com.observacao.model.Usuario;
import br.com.observacao.service.SolicitacaoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TerminalMenuTest {

    @Mock
    private SolicitacaoService solicitacaoService;

    @InjectMocks
    private TerminalMenu terminalMenu;

    private InputStream originalIn;
    private PrintStream originalOut;
    private ByteArrayOutputStream outputCapturado;

    private record Captura(Solicitacao solicitacao, Usuario usuario) {}

    @BeforeEach
    void setUp() {
        originalIn = System.in;
        originalOut = System.out;
        outputCapturado = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputCapturado, true, UTF_8));
    }

    @AfterEach
    void tearDown() {
        System.setIn(originalIn);
        System.setOut(originalOut);
    }

    // ---------- helpers ----------

    private void simularEntrada(String... linhas) {
        String entrada = String.join("\n", linhas) + "\n";
        System.setIn(new ByteArrayInputStream(entrada.getBytes(UTF_8)));
    }

    private String saida() {
        return outputCapturado.toString(UTF_8);
    }

    private int contarOcorrencias(String texto, String trecho) {
        int contador = 0;
        int indice = 0;
        while ((indice = texto.indexOf(trecho, indice)) != -1) {
            contador++;
            indice += trecho.length();
        }
        return contador;
    }

    /**
     * Monta a sequencia de entradas da opcao 1 (registrar), na ordem em que o menu le:
     * menu, categoria, [categoria custom], prioridade, descricao, logradouro, bairro,
     * referencia, anonimo S/N, [nome, email], saida.
     */
    private String[] entradaRegistrar(String categoria, String categoriaCustom, String prioridade,
                                      String anonimo, String nome, String email) {
        List<String> linhas = new ArrayList<>(List.of("1", categoria));
        if (categoriaCustom != null) {
            linhas.add(categoriaCustom);
        }
        linhas.addAll(List.of(prioridade, "Descricao qualquer para o teste",
                "Rua A, 123", "Centro", "Perto da praca", anonimo));
        if (nome != null) {
            linhas.add(nome);
            linhas.add(email);
        }
        linhas.add("0");
        return linhas.toArray(new String[0]);
    }

    private Captura executarRegistrar(String categoria, String categoriaCustom, String prioridade,
                                      String anonimo, String nome, String email) throws Exception {
        simularEntrada(entradaRegistrar(categoria, categoriaCustom, prioridade, anonimo, nome, email));
        when(solicitacaoService.criarSolicitacao(any(Solicitacao.class), any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        terminalMenu.run();

        ArgumentCaptor<Solicitacao> solCaptor = ArgumentCaptor.forClass(Solicitacao.class);
        ArgumentCaptor<Usuario> usrCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(solicitacaoService, times(1)).criarSolicitacao(solCaptor.capture(), usrCaptor.capture());
        return new Captura(solCaptor.getValue(), usrCaptor.getValue());
    }

    /** Atalho: registro anonimo (resposta "S"), sem perguntar nome/email. */
    private Captura executarRegistrarAnonimo(String categoria, String categoriaCustom, String prioridade)
            throws Exception {
        return executarRegistrar(categoria, categoriaCustom, prioridade, "S", null, null);
    }

    private Solicitacao criarSolicitacaoCompleta() {
        Solicitacao s = new Solicitacao();
        s.setCodigo("REQ-2026-AAAA");
        s.setCategoria("Iluminacao");
        s.setPrioridade("Alta");
        s.setStatus("Aberto");
        s.setData("11/09/2026");
        s.setSla("5 dias");
        s.setDescricao("Poste de luz apagado na rua principal");
        s.setAnonimo(false);
        s.setCidadao(new Usuario("Joao Silva", "joao@email.com"));

        Endereco e = new Endereco();
        e.setLogradouro("Rua A, 123");
        e.setBairro("Centro");
        e.setPontoReferencia("Perto da praca");
        s.setEndereco(e);

        s.getHistorico().add(new Atualizacao("11/09/2026 10:00", "Solicitacao registrada pelo cidadao", "Sistema"));
        return s;
    }

    // ---------- run / menu ----------

    @Test
    @DisplayName("run: opcao 0 imediata encerra sem chamar o service")
    void run_opcaoZeroImediata_encerraSemChamarService() throws Exception {
        simularEntrada("0");

        terminalMenu.run();

        assertTrue(saida().contains("Programa encerrado."));
        verifyNoInteractions(solicitacaoService);
    }

    @Test
    @DisplayName("run: opcao invalida imprime mensagem e o loop continua ate receber 0")
    void run_opcaoInvalida_imprimeMensagemEContinuaLoop() throws Exception {
        simularEntrada("99", "0");

        terminalMenu.run();

        assertTrue(saida().contains("Opcao invalida."));
        assertTrue(saida().contains("Programa encerrado."));
        verifyNoInteractions(solicitacaoService);
    }

    @Test
    @DisplayName("run: menu eh exibido a cada iteracao do loop")
    void run_menuExibidoACadaIteracao() throws Exception {
        simularEntrada("99", "99", "0");

        terminalMenu.run();

        assertEquals(3, contarOcorrencias(saida(), "=== ObservaAcao - ODS ==="));
    }

    // ---------- lerInteiro ----------

    @Test
    @DisplayName("lerInteiro: entrada valida na primeira tentativa nao imprime erro")
    void lerInteiro_entradaValida_naoImprimeErro() throws Exception {
        simularEntrada("0");

        terminalMenu.run();

        assertFalse(saida().contains("Entrada invalida"));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 5})
    @DisplayName("lerInteiro: N entradas invalidas seguidas repetem a mensagem N vezes")
    void lerInteiro_entradasInvalidasSeguidas_repeteMensagemParaCadaTentativa(int tentativasInvalidas) throws Exception {
        List<String> linhas = new ArrayList<>();
        for (int i = 0; i < tentativasInvalidas; i++) {
            linhas.add("abc");
        }
        linhas.add("0");
        simularEntrada(linhas.toArray(new String[0]));

        terminalMenu.run();

        assertEquals(tentativasInvalidas, contarOcorrencias(saida(), "Entrada invalida. Digite um numero: "));
        assertTrue(saida().contains("Programa encerrado."));
    }

    @Test
    @DisplayName("lerInteiro: entrada invalida no meio do registro eh repetida sem perder o fluxo")
    void lerInteiro_entradaInvalidaNaCategoria_repeteEContinuaRegistro() throws Exception {
        simularEntrada("1", "abc", "1", "1", "Descricao qualquer para o teste",
                "Rua A, 123", "Centro", "Perto da praca", "S", "0");
        when(solicitacaoService.criarSolicitacao(any(Solicitacao.class), any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        terminalMenu.run();

        ArgumentCaptor<Solicitacao> captor = ArgumentCaptor.forClass(Solicitacao.class);
        verify(solicitacaoService).criarSolicitacao(captor.capture(), any(Usuario.class));
        assertEquals("Iluminacao", captor.getValue().getCategoria());
        assertEquals(1, contarOcorrencias(saida(), "Entrada invalida. Digite um numero: "));
    }

    // ---------- opcao 1: registrar ----------

    @ParameterizedTest
    @CsvSource({
            "1,Iluminacao",
            "2,Buraco na via",
            "3,Limpeza urbana",
            "4,Saude",
            "5,Seguranca Escolar"
    })
    @DisplayName("registrarSolicitacao: categorias 1 a 5 mapeiam para o nome correto sem prompt extra")
    void registrarSolicitacao_categoriaPredefinida_mapeiaSemPromptExtra(String opcao, String esperado) throws Exception {
        Captura c = executarRegistrarAnonimo(opcao, null, "1");

        assertEquals(esperado, c.solicitacao().getCategoria());
        assertFalse(saida().contains("Digite o nome da categoria"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"6", "0", "99"})
    @DisplayName("registrarSolicitacao: categoria fora de 1 a 5 pede o nome e usa a linha seguinte")
    void registrarSolicitacao_categoriaForaDoRange_usaNomeCustomizado(String opcao) throws Exception {
        Captura c = executarRegistrarAnonimo(opcao, "Nome Categoria Custom", "1");

        assertEquals("Nome Categoria Custom", c.solicitacao().getCategoria());
        assertTrue(saida().contains("Digite o nome da categoria (Outros): "));
    }

    @ParameterizedTest
    @CsvSource({
            "1,Alta",
            "2,Media",
            "3,Baixa",
            "9,Baixa"
    })
    @DisplayName("registrarSolicitacao: prioridade 1 a 3 mapeia direto, fora do range cai em Baixa")
    void registrarSolicitacao_prioridade_mapeiaCorretamente(String opcao, String esperado) throws Exception {
        Captura c = executarRegistrarAnonimo("1", null, opcao);

        assertEquals(esperado, c.solicitacao().getPrioridade());
    }

    @Test
    @DisplayName("registrarSolicitacao: descricao e endereco digitados chegam na solicitacao")
    void registrarSolicitacao_preencheDescricaoEEndereco() throws Exception {
        Captura c = executarRegistrarAnonimo("1", null, "1");

        assertEquals("Descricao qualquer para o teste", c.solicitacao().getDescricao());
        assertNotNull(c.solicitacao().getEndereco());
        assertEquals("Rua A, 123", c.solicitacao().getEndereco().getLogradouro());
        assertEquals("Centro", c.solicitacao().getEndereco().getBairro());
        assertEquals("Perto da praca", c.solicitacao().getEndereco().getPontoReferencia());
    }

    @Test
    @DisplayName("registrarSolicitacao: adiciona a atualizacao de abertura no historico")
    void registrarSolicitacao_adicionaAtualizacaoDeAbertura() throws Exception {
        Captura c = executarRegistrarAnonimo("1", null, "1");

        List<Atualizacao> historico = c.solicitacao().getHistorico();
        assertEquals(1, historico.size());
        assertEquals("Solicitacao registrada pelo cidadao", historico.get(0).getNota());
        assertEquals("Sistema", historico.get(0).getAutor());
        assertTrue(historico.get(0).getDataHora().matches("\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}"),
                "Formato de dataHora inesperado: " + historico.get(0).getDataHora());
    }

    @ParameterizedTest
    @ValueSource(strings = {"S", "s"})
    @DisplayName("registrarSolicitacao: resposta S/s marca anonimo e nao pergunta nome nem e-mail")
    void registrarSolicitacao_respostaAnonimo_marcaAnonimoESemDadosDoCidadao(String resposta) throws Exception {
        Captura c = executarRegistrar("1", null, "1", resposta, null, null);

        assertTrue(c.solicitacao().isAnonimo());
        assertNull(c.usuario().getNome());
        assertNull(c.usuario().getEmail());
        assertFalse(saida().contains("Nome completo"));
        assertFalse(saida().contains("E-mail:"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"N", "n", "qualquer coisa", ""})
    @DisplayName("registrarSolicitacao: qualquer resposta diferente de S pede nome e e-mail e repassa ao service")
    void registrarSolicitacao_respostaNaoAnonimo_pedeNomeEEmail(String resposta) throws Exception {
        Captura c = executarRegistrar("1", null, "1", resposta, "Joao Silva", "joao@email.com");

        assertFalse(c.solicitacao().isAnonimo());
        assertEquals("Joao Silva", c.usuario().getNome());
        assertEquals("joao@email.com", c.usuario().getEmail());
        assertTrue(saida().contains("Nome completo: "));
        assertTrue(saida().contains("E-mail: "));
    }

    @Test
    @DisplayName("registrarSolicitacao: sucesso imprime mensagem de confirmacao")
    void registrarSolicitacao_sucesso_imprimeMensagemDeConfirmacao() throws Exception {
        executarRegistrarAnonimo("1", null, "1");

        assertTrue(saida().contains("Solicitacao registrada com sucesso e vinculada ao banco de dados!"));
    }

    @Test
    @DisplayName("registrarSolicitacao: falha do service imprime erro e nao propaga excecao")
    void registrarSolicitacao_servicoLancaExcecao_imprimeErroENaoPropaga() throws Exception {
        simularEntrada(entradaRegistrar("1", null, "1", "S", null, null));
        when(solicitacaoService.criarSolicitacao(any(Solicitacao.class), any(Usuario.class)))
                .thenThrow(new RuntimeException("Falha ao salvar"));

        assertDoesNotThrow(() -> terminalMenu.run());

        assertTrue(saida().contains("Erro ao registrar: Falha ao salvar"));
        assertFalse(saida().contains("registrada com sucesso"));
    }

    // ---------- opcao 2: listar ----------

    @Test
    @DisplayName("listarSolicitacoes: imprime uma linha formatada para cada solicitacao")
    void listarSolicitacoes_comMultiplosElementos_imprimeLinhaParaCadaUm() throws Exception {
        Solicitacao s1 = new Solicitacao();
        s1.setCodigo("REQ-1");
        s1.setCategoria("Iluminacao");
        s1.setStatus("Aberto");

        Solicitacao s2 = new Solicitacao();
        s2.setCodigo("REQ-2");
        s2.setCategoria("Saude");
        s2.setStatus("Concluido");

        when(solicitacaoService.listarTodas()).thenReturn(List.of(s1, s2));
        simularEntrada("2", "0");

        terminalMenu.run();

        String saida = saida();
        assertTrue(saida.contains("Protocolo: REQ-1 | Categoria: Iluminacao | Status: Aberto"));
        assertTrue(saida.contains("Protocolo: REQ-2 | Categoria: Saude | Status: Concluido"));
        verify(solicitacaoService, times(1)).listarTodas();
    }

    @Test
    @DisplayName("listarSolicitacoes: lista vazia imprime so o cabecalho")
    void listarSolicitacoes_listaVazia_naoImprimeProtocolo() throws Exception {
        when(solicitacaoService.listarTodas()).thenReturn(Collections.emptyList());
        simularEntrada("2", "0");

        terminalMenu.run();

        assertTrue(saida().contains("--- Lista de Solicitacoes ---"));
        assertFalse(saida().contains("Protocolo:"));
    }

    // ---------- opcao 3: atualizar status ----------

    @Test
    @DisplayName("atualizarStatus: chama o service com codigo e status digitados e confirma")
    void atualizarStatus_sucesso_chamaServiceEImprimeConfirmacao() throws Exception {
        simularEntrada("3", "REQ-1", "Concluido", "0");
        when(solicitacaoService.atualizarStatus("REQ-1", "Concluido")).thenReturn(new Solicitacao());

        terminalMenu.run();

        verify(solicitacaoService, times(1)).atualizarStatus("REQ-1", "Concluido");
        assertTrue(saida().contains("Status atualizado com sucesso!"));
    }

    @Test
    @DisplayName("atualizarStatus: falha do service imprime erro e nao confirma")
    void atualizarStatus_servicoLancaExcecao_imprimeErro() throws Exception {
        simularEntrada("3", "REQ-INEXISTENTE", "Concluido", "0");
        when(solicitacaoService.atualizarStatus("REQ-INEXISTENTE", "Concluido"))
                .thenThrow(new RuntimeException("Protocolo nao encontrado"));

        terminalMenu.run();

        assertTrue(saida().contains("Erro ao atualizar: Protocolo nao encontrado"));
        assertFalse(saida().contains("Status atualizado com sucesso!"));
    }

    // ---------- opcao 4: excluir ----------

    @Test
    @DisplayName("excluirSolicitacao: chama o service com o codigo digitado e confirma")
    void excluirSolicitacao_sucesso_chamaServiceEImprimeConfirmacao() throws Exception {
        simularEntrada("4", "REQ-1", "0");

        terminalMenu.run();

        verify(solicitacaoService, times(1)).excluirSolicitacao("REQ-1");
        assertTrue(saida().contains("Solicitacao excluida com sucesso!"));
    }

    @Test
    @DisplayName("excluirSolicitacao: falha do service imprime erro e nao confirma")
    void excluirSolicitacao_servicoLancaExcecao_imprimeErro() throws Exception {
        simularEntrada("4", "REQ-INEXISTENTE", "0");
        doThrow(new RuntimeException("Protocolo nao encontrado"))
                .when(solicitacaoService).excluirSolicitacao("REQ-INEXISTENTE");

        terminalMenu.run();

        assertTrue(saida().contains("Erro ao excluir: Protocolo nao encontrado"));
        assertFalse(saida().contains("excluida com sucesso"));
    }

    // ---------- opcao 5: detalhes ----------

    @Test
    @DisplayName("verDetalhesSolicitacao: busca pelo codigo digitado e imprime todos os campos")
    void verDetalhesSolicitacao_sucesso_imprimeTodosOsCampos() throws Exception {
        when(solicitacaoService.buscarPorCodigo("REQ-2026-AAAA")).thenReturn(criarSolicitacaoCompleta());
        simularEntrada("5", "REQ-2026-AAAA", "0");

        terminalMenu.run();

        String saida = saida();
        assertTrue(saida.contains("DETALHES DA SOLICITAÇÃO: REQ-2026-AAAA"));
        assertTrue(saida.contains("Categoria..: Iluminacao"));
        assertTrue(saida.contains("Prioridade.: Alta (SLA: 5 dias)"));
        assertTrue(saida.contains("Status.....: Aberto"));
        assertTrue(saida.contains("Data.......: 11/09/2026"));
        assertTrue(saida.contains("Descricao..: Poste de luz apagado na rua principal"));
        assertTrue(saida.contains("Nome.......: Joao Silva"));
        assertTrue(saida.contains("E-mail.....: joao@email.com"));
        assertTrue(saida.contains("Logradouro.: Rua A, 123"));
        assertTrue(saida.contains("Bairro.....: Centro"));
        assertTrue(saida.contains("Referência.: Perto da praca"));
        assertTrue(saida.contains("- [11/09/2026 10:00] Solicitacao registrada pelo cidadao (Autor: Sistema)"));
        verify(solicitacaoService, times(1)).buscarPorCodigo("REQ-2026-AAAA");
    }

    @Test
    @DisplayName("verDetalhesSolicitacao: campos principais sao impressos na ordem correta")
    void verDetalhesSolicitacao_camposNaOrdemCorreta() throws Exception {
        when(solicitacaoService.buscarPorCodigo("REQ-2026-AAAA")).thenReturn(criarSolicitacaoCompleta());
        simularEntrada("5", "REQ-2026-AAAA", "0");

        terminalMenu.run();

        String saida = saida();
        String[] rotulos = {
                "DETALHES DA SOLICITAÇÃO: REQ-2026-AAAA",
                "Categoria..: ",
                "Prioridade.: ",
                "Status.....: ",
                "Data.......: ",
                "Descricao..: ",
                "DADOS DO CIDADÃO",
                "LOCALIZAÇÃO",
                "HISTÓRICO DE ATUALIZAÇÕES"
        };
        int anterior = -1;
        for (String rotulo : rotulos) {
            int pos = saida.indexOf(rotulo);
            assertTrue(pos >= 0, "Nao encontrado na saida: " + rotulo);
            assertTrue(pos > anterior, "Fora de ordem: " + rotulo);
            anterior = pos;
        }
    }

    @Test
    @DisplayName("verDetalhesSolicitacao: anonimo imprime 'Anônimo' e nao mostra nome nem e-mail")
    void verDetalhesSolicitacao_anonimo_naoMostraDadosDoCidadao() throws Exception {
        Solicitacao s = criarSolicitacaoCompleta();
        s.setAnonimo(true);
        s.setCidadao(new Usuario("Anonimo", "anonimo@sistema.com"));
        when(solicitacaoService.buscarPorCodigo("REQ-2026-AAAA")).thenReturn(s);
        simularEntrada("5", "REQ-2026-AAAA", "0");

        terminalMenu.run();

        String saida = saida();
        assertTrue(saida.contains("Registro...: Anônimo"));
        assertFalse(saida.contains("Nome......."));
        assertFalse(saida.contains("anonimo@sistema.com"));
    }

    @Test
    @DisplayName("verDetalhesSolicitacao: endereco null imprime 'Endereço não informado.'")
    void verDetalhesSolicitacao_enderecoNull_imprimeMensagem() throws Exception {
        Solicitacao s = criarSolicitacaoCompleta();
        s.setEndereco(null);
        when(solicitacaoService.buscarPorCodigo("REQ-2026-AAAA")).thenReturn(s);
        simularEntrada("5", "REQ-2026-AAAA", "0");

        terminalMenu.run();

        assertTrue(saida().contains("Endereço não informado."));
        assertFalse(saida().contains("Logradouro."));
    }

    @Test
    @DisplayName("verDetalhesSolicitacao: historico vazio imprime 'Sem histórico registrado.'")
    void verDetalhesSolicitacao_historicoVazio_imprimeMensagem() throws Exception {
        Solicitacao s = criarSolicitacaoCompleta();
        s.setHistorico(new ArrayList<>());
        when(solicitacaoService.buscarPorCodigo("REQ-2026-AAAA")).thenReturn(s);
        simularEntrada("5", "REQ-2026-AAAA", "0");

        terminalMenu.run();

        assertTrue(saida().contains("Sem histórico registrado."));
    }

    @Test
    @DisplayName("verDetalhesSolicitacao: historico null imprime 'Sem histórico registrado.'")
    void verDetalhesSolicitacao_historicoNull_imprimeMensagem() throws Exception {
        Solicitacao s = criarSolicitacaoCompleta();
        s.setHistorico(null);
        when(solicitacaoService.buscarPorCodigo("REQ-2026-AAAA")).thenReturn(s);
        simularEntrada("5", "REQ-2026-AAAA", "0");

        terminalMenu.run();

        assertTrue(saida().contains("Sem histórico registrado."));
    }

    @Test
    @DisplayName("verDetalhesSolicitacao: multiplas atualizacoes sao impressas na ordem do historico")
    void verDetalhesSolicitacao_multiplasAtualizacoes_imprimeEmOrdem() throws Exception {
        Solicitacao s = criarSolicitacaoCompleta();
        s.getHistorico().add(new Atualizacao("12/09/2026 09:30", "Status alterado para: Concluido", "Gestor Público"));
        when(solicitacaoService.buscarPorCodigo("REQ-2026-AAAA")).thenReturn(s);
        simularEntrada("5", "REQ-2026-AAAA", "0");

        terminalMenu.run();

        String saida = saida();
        int primeira = saida.indexOf("- [11/09/2026 10:00]");
        int segunda = saida.indexOf("- [12/09/2026 09:30] Status alterado para: Concluido (Autor: Gestor Público)");
        assertTrue(primeira >= 0);
        assertTrue(segunda > primeira);
    }

    @Test
    @DisplayName("verDetalhesSolicitacao: falha do service imprime 'Erro: ...'")
    void verDetalhesSolicitacao_servicoLancaExcecao_imprimeErro() throws Exception {
        when(solicitacaoService.buscarPorCodigo("INEXISTENTE"))
                .thenThrow(new RuntimeException("Protocolo nao encontrado"));
        simularEntrada("5", "INEXISTENTE", "0");

        terminalMenu.run();

        assertTrue(saida().contains("Erro: Protocolo nao encontrado"));
        assertFalse(saida().contains("DETALHES DA SOLICITAÇÃO"));
    }
}