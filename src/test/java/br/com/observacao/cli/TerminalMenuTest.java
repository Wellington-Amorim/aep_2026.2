package br.com.observacao.cli;

import br.com.observacao.model.Solicitacao;
import br.com.observacao.service.SolicitacaoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @BeforeEach
    void setUp() {
        originalIn = System.in;
        originalOut = System.out;
        outputCapturado = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputCapturado));
    }

    @AfterEach
    void tearDown() {
        System.setIn(originalIn);
        System.setOut(originalOut);
    }

    private void simularEntrada(String... linhas) {
        String entrada = String.join("\n", linhas) + "\n";
        System.setIn(new ByteArrayInputStream(entrada.getBytes()));
    }

    private String saida() {
        return outputCapturado.toString();
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

    private Solicitacao criarSolicitacaoCompleta() {
        Solicitacao s = new Solicitacao();
        s.setCodigo("REQ-2026-AAAA");
        s.setCategoria("Iluminacao");
        s.setPrioridade("Alta");
        s.setStatus("Aberto");
        s.setData("11/09/2026");
        s.setSla("5 dias");
        s.setDescricao("Poste de luz apagado na rua principal");
        return s;
    }

    private Solicitacao executarRegistrar(String categoriaOpcao, String nomeCustomizado,
                                          String prioridadeOpcao, String descricao) throws Exception {
        List<String> linhas = new ArrayList<>();
        linhas.add("1");
        linhas.add(categoriaOpcao);
        if (nomeCustomizado != null) {
            linhas.add(nomeCustomizado);
        }
        linhas.add(prioridadeOpcao);
        linhas.add(descricao);
        linhas.add("0");
        simularEntrada(linhas.toArray(new String[0]));

        when(solicitacaoService.criarSolicitacao(any(Solicitacao.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        terminalMenu.run();

        ArgumentCaptor<Solicitacao> captor = ArgumentCaptor.forClass(Solicitacao.class);
        verify(solicitacaoService, times(1)).criarSolicitacao(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("run: opcao 0 imediata encerra sem chamar nenhum metodo do service")
    void run_opcaoZeroImediata_encerraSemChamarService() throws Exception {
        simularEntrada("0");

        terminalMenu.run();

        assertTrue(saida().contains("Programa encerrado."));
        verifyNoInteractions(solicitacaoService);
    }

    @Test
    @DisplayName("run: opcao 1 chama registrarSolicitacao via criarSolicitacao do service")
    void run_opcao1_chamaRegistrarSolicitacao() throws Exception {
        simularEntrada("1", "1", "1", "Descricao qualquer para o teste", "0");
        when(solicitacaoService.criarSolicitacao(any(Solicitacao.class))).thenReturn(new Solicitacao());

        terminalMenu.run();

        verify(solicitacaoService, times(1)).criarSolicitacao(any(Solicitacao.class));
    }

    @Test
    @DisplayName("run: opcao 2 chama listarSolicitacoes via listarTodas do service")
    void run_opcao2_chamaListarSolicitacoes() throws Exception {
        simularEntrada("2", "0");
        when(solicitacaoService.listarTodas()).thenReturn(Collections.emptyList());

        terminalMenu.run();

        verify(solicitacaoService, times(1)).listarTodas();
    }

    @Test
    @DisplayName("run: opcao 3 chama atualizarStatus do service com os argumentos digitados")
    void run_opcao3_chamaAtualizarStatus() throws Exception {
        simularEntrada("3", "REQ-1", "NovoStatus", "0");
        when(solicitacaoService.atualizarStatus("REQ-1", "NovoStatus")).thenReturn(new Solicitacao());

        terminalMenu.run();

        verify(solicitacaoService, times(1)).atualizarStatus("REQ-1", "NovoStatus");
    }

    @Test
    @DisplayName("run: opcao 4 chama excluirPorCodigo do service com o codigo digitado")
    void run_opcao4_chamaExcluirSolicitacao() throws Exception {
        simularEntrada("4", "REQ-1", "0");

        terminalMenu.run();

        verify(solicitacaoService, times(1)).excluirPorCodigo("REQ-1");
    }

    @Test
    @DisplayName("run: opcao 5 chama buscarPorCodigo do service com o codigo digitado")
    void run_opcao5_chamaVerDetalhesSolicitacao() throws Exception {
        simularEntrada("5", "REQ-1", "0");
        when(solicitacaoService.buscarPorCodigo("REQ-1")).thenReturn(criarSolicitacaoCompleta());

        terminalMenu.run();

        verify(solicitacaoService, times(1)).buscarPorCodigo("REQ-1");
    }

    @Test
    @DisplayName("run: opcao invalida imprime 'Opcao invalida.' e o loop continua ate receber 0")
    void run_opcaoInvalida_imprimeMensagemEContinuaLoop() throws Exception {
        simularEntrada("99", "0");

        terminalMenu.run();

        assertTrue(saida().contains("Opcao invalida."));
        assertTrue(saida().contains("Programa encerrado."));
        verifyNoInteractions(solicitacaoService);
    }

    @Test
    @DisplayName("lerInteiro: entrada valida na primeira tentativa nao imprime mensagem de erro")
    void lerInteiro_entradaValidaPrimeiraTentativa_naoImprimeMensagemDeErro() throws Exception {
        simularEntrada("0");

        terminalMenu.run();

        assertFalse(saida().contains("Entrada invalida"));
    }

    @Test
    @DisplayName("lerInteiro: entrada invalida seguida de valida repete a mensagem de erro uma vez")
    void lerInteiro_entradaInvalidaSeguidaDeValida_repeteMensagemDeErroUmaVez() throws Exception {
        simularEntrada("abc", "0");

        terminalMenu.run();

        String saida = saida();
        assertEquals(1, contarOcorrencias(saida, "Entrada invalida. Digite um numero: "));
        assertTrue(saida.contains("Programa encerrado."));
    }

    @Test
    @DisplayName("lerInteiro: multiplas entradas invalidas seguidas repetem a mensagem de erro para cada tentativa")
    void lerInteiro_multiplasEntradasInvalidasSeguidas_repeteMensagemDeErroParaCadaTentativa() throws Exception {
        simularEntrada("abc", "xyz", "!!!", "0");

        terminalMenu.run();

        assertEquals(3, contarOcorrencias(saida(), "Entrada invalida. Digite um numero: "));
    }

    @Test
    @DisplayName("registrarSolicitacao: categoria 1 mapeia para Iluminacao sem prompt extra")
    void registrarSolicitacao_categoria1_mapeiaIluminacaoSemPromptExtra() throws Exception {
        Solicitacao resultado = executarRegistrar("1", null, "1", "Descricao qualquer para o teste");

        assertEquals("Iluminacao", resultado.getCategoria());
        assertFalse(saida().contains("Digite o nome da categoria"));
    }

    @Test
    @DisplayName("registrarSolicitacao: categoria 2 mapeia para Buraco na via sem prompt extra")
    void registrarSolicitacao_categoria2_mapeiaBuracoNaViaSemPromptExtra() throws Exception {
        Solicitacao resultado = executarRegistrar("2", null, "1", "Descricao qualquer para o teste");

        assertEquals("Buraco na via", resultado.getCategoria());
        assertFalse(saida().contains("Digite o nome da categoria"));
    }

    @Test
    @DisplayName("registrarSolicitacao: categoria 3 mapeia para Limpeza urbana sem prompt extra")
    void registrarSolicitacao_categoria3_mapeiaLimpezaUrbanaSemPromptExtra() throws Exception {
        Solicitacao resultado = executarRegistrar("3", null, "1", "Descricao qualquer para o teste");

        assertEquals("Limpeza urbana", resultado.getCategoria());
        assertFalse(saida().contains("Digite o nome da categoria"));
    }

    @Test
    @DisplayName("registrarSolicitacao: categoria 4 mapeia para Saude sem prompt extra")
    void registrarSolicitacao_categoria4_mapeiaSaudeSemPromptExtra() throws Exception {
        Solicitacao resultado = executarRegistrar("4", null, "1", "Descricao qualquer para o teste");

        assertEquals("Saude", resultado.getCategoria());
        assertFalse(saida().contains("Digite o nome da categoria"));
    }

    @Test
    @DisplayName("registrarSolicitacao: categoria 5 mapeia para Seguranca Escolar sem prompt extra")
    void registrarSolicitacao_categoria5_mapeiaSegurancaEscolarSemPromptExtra() throws Exception {
        Solicitacao resultado = executarRegistrar("5", null, "1", "Descricao qualquer para o teste");

        assertEquals("Seguranca Escolar", resultado.getCategoria());
        assertFalse(saida().contains("Digite o nome da categoria"));
    }

    @Test
    @DisplayName("registrarSolicitacao: categoria fora do range aciona prompt extra e usa a linha seguinte como nome customizado")
    void registrarSolicitacao_categoriaForaDoRange_acionaPromptExtraEUsaNomeCustomizado() throws Exception {
        Solicitacao resultado = executarRegistrar("6", "Nome Categoria Custom", "1", "Descricao qualquer para o teste");

        assertEquals("Nome Categoria Custom", resultado.getCategoria());
        assertTrue(saida().contains("Digite o nome da categoria (Outros): "));
    }

    @Test
    @DisplayName("registrarSolicitacao: prioridade 1 mapeia para Alta")
    void registrarSolicitacao_prioridade1_mapeiaAlta() throws Exception {
        Solicitacao resultado = executarRegistrar("1", null, "1", "Descricao qualquer para o teste");

        assertEquals("Alta", resultado.getPrioridade());
    }

    @Test
    @DisplayName("registrarSolicitacao: prioridade 2 mapeia para Media")
    void registrarSolicitacao_prioridade2_mapeiaMedia() throws Exception {
        Solicitacao resultado = executarRegistrar("1", null, "2", "Descricao qualquer para o teste");

        assertEquals("Media", resultado.getPrioridade());
    }

    @Test
    @DisplayName("registrarSolicitacao: prioridade 3 mapeia para Baixa")
    void registrarSolicitacao_prioridade3_mapeiaBaixa() throws Exception {
        Solicitacao resultado = executarRegistrar("1", null, "3", "Descricao qualquer para o teste");

        assertEquals("Baixa", resultado.getPrioridade());
    }

    @Test
    @DisplayName("registrarSolicitacao: prioridade fora do range cai silenciosamente em Baixa sem prompt extra")
    void registrarSolicitacao_prioridadeForaDoRange_caiSilenciosamenteEmBaixa() throws Exception {
        Solicitacao resultado = executarRegistrar("1", null, "9", "Descricao qualquer para o teste");

        assertEquals("Baixa", resultado.getPrioridade());
    }

    @Test
    @DisplayName("registrarSolicitacao: anonimo eh sempre definido como false")
    void registrarSolicitacao_qualquerFluxo_anonimoSempreFalse() throws Exception {
        Solicitacao resultado = executarRegistrar("1", null, "1", "Descricao qualquer para o teste");

        assertFalse(resultado.isAnonimo());
    }

    @Test
    @DisplayName("registrarSolicitacao: sucesso imprime mensagem de confirmacao")
    void registrarSolicitacao_sucesso_imprimeMensagemDeConfirmacao() throws Exception {
        executarRegistrar("1", null, "1", "Descricao qualquer para o teste");

        assertTrue(saida().contains("Solicitacao registrada com sucesso!"));
    }

    @Test
    @DisplayName("registrarSolicitacao: falha do service imprime mensagem de erro e nao propaga excecao")
    void registrarSolicitacao_servicoLancaExcecao_imprimeErroENaoPropaga() throws Exception {
        simularEntrada("1", "1", "1", "Descricao qualquer para o teste", "0");
        when(solicitacaoService.criarSolicitacao(any(Solicitacao.class)))
                .thenThrow(new RuntimeException("Falha ao salvar"));

        assertDoesNotThrow(() -> terminalMenu.run());

        assertTrue(saida().contains("Erro ao registrar: Falha ao salvar"));
    }

    // ---------- listarSolicitacoes ----------

    @Test
    @DisplayName("listarSolicitacoes: lista com multiplos elementos imprime cada linha formatada corretamente")
    void listarSolicitacoes_comMultiplosElementos_imprimeLinhaFormatadaParaCadaUm() throws Exception {
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
    }

    @Test
    @DisplayName("listarSolicitacoes: lista vazia nao imprime nenhuma linha de protocolo")
    void listarSolicitacoes_listaVazia_naoImprimeNenhumaLinhaDeProtocolo() throws Exception {
        when(solicitacaoService.listarTodas()).thenReturn(Collections.emptyList());
        simularEntrada("2", "0");

        terminalMenu.run();

        assertFalse(saida().contains("Protocolo:"));
        assertTrue(saida().contains("--- Lista de Solicitacoes ---"));
    }

    @Test
    @DisplayName("atualizarStatus: sucesso imprime mensagem de confirmacao")
    void atualizarStatus_sucesso_imprimeMensagemDeConfirmacao() throws Exception {
        simularEntrada("3", "REQ-1", "Concluido", "0");
        when(solicitacaoService.atualizarStatus("REQ-1", "Concluido")).thenReturn(new Solicitacao());

        terminalMenu.run();

        assertTrue(saida().contains("Status atualizado com sucesso!"));
    }

    @Test
    @DisplayName("atualizarStatus: falha do service imprime mensagem de erro")
    void atualizarStatus_servicoLancaExcecao_imprimeMensagemDeErro() throws Exception {
        simularEntrada("3", "REQ-INEXISTENTE", "Concluido", "0");
        when(solicitacaoService.atualizarStatus("REQ-INEXISTENTE", "Concluido"))
                .thenThrow(new RuntimeException("Protocolo nao encontrado"));

        terminalMenu.run();

        assertTrue(saida().contains("Erro ao atualizar: Protocolo nao encontrado"));
    }

    @Test
    @DisplayName("excluirSolicitacao: sucesso imprime mensagem de confirmacao")
    void excluirSolicitacao_sucesso_imprimeMensagemDeConfirmacao() throws Exception {
        simularEntrada("4", "REQ-1", "0");

        terminalMenu.run();

        assertTrue(saida().contains("Solicitacao excluida com sucesso!"));
        verify(solicitacaoService, times(1)).excluirPorCodigo("REQ-1");
    }

    @Test
    @DisplayName("excluirSolicitacao: falha do service imprime mensagem de erro")
    void excluirSolicitacao_servicoLancaExcecao_imprimeMensagemDeErro() throws Exception {
        simularEntrada("4", "REQ-INEXISTENTE", "0");
        doThrow(new RuntimeException("Protocolo nao encontrado"))
                .when(solicitacaoService).excluirPorCodigo("REQ-INEXISTENTE");

        terminalMenu.run();

        assertTrue(saida().contains("Erro ao excluir: Protocolo nao encontrado"));
    }

    @Test
    @DisplayName("verDetalhesSolicitacao: sucesso imprime todos os 7 campos na ordem correta")
    void verDetalhesSolicitacao_sucesso_imprimeTodosOsCamposNaOrdemCorreta() throws Exception {
        Solicitacao s = criarSolicitacaoCompleta();
        when(solicitacaoService.buscarPorCodigo("REQ-2026-AAAA")).thenReturn(s);
        simularEntrada("5", "REQ-2026-AAAA", "0");

        terminalMenu.run();

        String saida = saida();
        int posCodigo = saida.indexOf("Protocolo: REQ-2026-AAAA");
        int posCategoria = saida.indexOf("Categoria: Iluminacao");
        int posPrioridade = saida.indexOf("Prioridade: Alta");
        int posStatus = saida.indexOf("Status: Aberto");
        int posData = saida.indexOf("Data: 11/09/2026");
        int posSla = saida.indexOf("SLA: 5 dias");
        int posDescricao = saida.indexOf("Descricao: Poste de luz apagado na rua principal");

        assertTrue(posCodigo >= 0, "Campo Protocolo nao encontrado na saida");
        assertTrue(posCategoria > posCodigo, "Campo Categoria fora de ordem");
        assertTrue(posPrioridade > posCategoria, "Campo Prioridade fora de ordem");
        assertTrue(posStatus > posPrioridade, "Campo Status fora de ordem");
        assertTrue(posData > posStatus, "Campo Data fora de ordem");
        assertTrue(posSla > posData, "Campo SLA fora de ordem");
        assertTrue(posDescricao > posSla, "Campo Descricao fora de ordem");
    }

    @Test
    @DisplayName("verDetalhesSolicitacao: falha do service imprime mensagem de erro")
    void verDetalhesSolicitacao_servicoLancaExcecao_imprimeMensagemDeErro() throws Exception {
        when(solicitacaoService.buscarPorCodigo("INEXISTENTE"))
                .thenThrow(new RuntimeException("Protocolo nao encontrado"));
        simularEntrada("5", "INEXISTENTE", "0");

        terminalMenu.run();

        assertTrue(saida().contains("Erro ao buscar: Protocolo nao encontrado"));
    }
}