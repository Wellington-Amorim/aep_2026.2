package br.com.observacao.service;

import br.com.observacao.model.Atualizacao;
import br.com.observacao.model.Solicitacao;
import br.com.observacao.model.Usuario;
import br.com.observacao.repository.SolicitacaoRepository;
import br.com.observacao.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SolicitacaoServiceTest {

    private static final String EMAIL_ANONIMO = "anonimo@sistema.com";
    private static final String MSG_DESCRICAO_ANONIMA = "Denuncias anonimas exigem no minimo 20 caracteres";

    @Mock
    private SolicitacaoRepository repository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private SolicitacaoService service;

    private Usuario usuarioCli;

    @BeforeEach
    void setUp() {
        usuarioCli = new Usuario("Joao", "joao@email.com");

        // defaults usados pela maioria dos testes; cada teste sobrescreve o que precisar
        lenient().when(repository.save(any(Solicitacao.class))).thenAnswer(returnsFirstArg());
        lenient().when(usuarioRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(new Usuario("Existente", "existente@email.com")));
    }

    private Solicitacao novaSolicitacao(boolean anonimo, String descricao, String prioridade) {
        Solicitacao s = new Solicitacao();
        s.setAnonimo(anonimo);
        s.setDescricao(descricao);
        s.setPrioridade(prioridade);
        s.setCategoria("Categoria X");
        return s;
    }

    // ---------- criarSolicitacao: validacao (anonimo) ----------

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"curta", "abcdefghijklmnopqrs"}) // 19 caracteres no segundo
    @DisplayName("criarSolicitacao: anonimo com descricao null, vazia ou menor que 20 caracteres lanca IllegalArgumentException")
    void criarSolicitacao_anonimoComDescricaoInvalida_lancaIllegalArgumentException(String descricao) {
        Solicitacao s = novaSolicitacao(true, descricao, "Alta");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.criarSolicitacao(s, null));

        assertEquals(MSG_DESCRICAO_ANONIMA, ex.getMessage());
        verify(repository, never()).save(any());
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    @DisplayName("criarSolicitacao: anonimo com descricao de exatamente 20 caracteres eh aceito")
    void criarSolicitacao_anonimoComDescricao20Caracteres_aceita() {
        Solicitacao s = novaSolicitacao(true, "abcdefghijklmnopqrst", "Alta");

        assertDoesNotThrow(() -> service.criarSolicitacao(s, null));

        verify(repository, times(1)).save(s);
    }

    // ---------- criarSolicitacao: usuario anonimo ----------

    @Test
    @DisplayName("criarSolicitacao: anonimo reaproveita o usuario anonimo existente e nao cria outro")
    void criarSolicitacao_anonimoComUsuarioAnonimoExistente_reaproveita() {
        Usuario anonimo = new Usuario("Anonimo", EMAIL_ANONIMO);
        when(usuarioRepository.findByEmail(EMAIL_ANONIMO)).thenReturn(Optional.of(anonimo));

        Solicitacao resultado = service.criarSolicitacao(novaSolicitacao(true, "a".repeat(20), "Alta"), null);

        assertSame(anonimo, resultado.getCidadao());
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("criarSolicitacao: anonimo sem usuario anonimo previo cria e vincula Anonimo / anonimo@sistema.com")
    void criarSolicitacao_anonimoSemUsuarioAnonimoPrevio_criaEVincula() {
        when(usuarioRepository.findByEmail(EMAIL_ANONIMO)).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(returnsFirstArg());

        Solicitacao resultado = service.criarSolicitacao(novaSolicitacao(true, "a".repeat(20), "Alta"), null);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository, times(1)).save(captor.capture());
        assertEquals("Anonimo", captor.getValue().getNome());
        assertEquals(EMAIL_ANONIMO, captor.getValue().getEmail());
        assertSame(captor.getValue(), resultado.getCidadao());
    }

    @Test
    @DisplayName("criarSolicitacao: anonimo ignora o usuario informado e nao consulta nem persiste os dados dele")
    void criarSolicitacao_anonimoComUsuarioInformado_ignoraDadosDoUsuario() {
        Usuario anonimo = new Usuario("Anonimo", EMAIL_ANONIMO);
        when(usuarioRepository.findByEmail(EMAIL_ANONIMO)).thenReturn(Optional.of(anonimo));

        Solicitacao resultado = service.criarSolicitacao(novaSolicitacao(true, "a".repeat(20), "Alta"), usuarioCli);

        assertEquals(EMAIL_ANONIMO, resultado.getCidadao().getEmail());
        verify(usuarioRepository, never()).findByEmail("joao@email.com");
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    // ---------- criarSolicitacao: usuario identificado ----------

    @Test
    @DisplayName("criarSolicitacao: nao anonimo com e-mail ja cadastrado vincula o usuario do banco e nao salva outro")
    void criarSolicitacao_naoAnonimoComUsuarioExistente_vinculaUsuarioDoBanco() {
        Usuario doBanco = new Usuario("Joao do Banco", "joao@email.com");
        doBanco.setId("abc123");
        when(usuarioRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(doBanco));

        Solicitacao resultado = service.criarSolicitacao(novaSolicitacao(false, "descricao", "Alta"), usuarioCli);

        assertSame(doBanco, resultado.getCidadao());
        verify(usuarioRepository, never()).save(any(Usuario.class));
        verify(usuarioRepository, never()).findByEmail(EMAIL_ANONIMO);
    }

    @Test
    @DisplayName("criarSolicitacao: nao anonimo com e-mail novo salva o usuario informado e vincula")
    void criarSolicitacao_naoAnonimoComUsuarioNovo_salvaEVincula() {
        when(usuarioRepository.findByEmail("joao@email.com")).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(returnsFirstArg());

        Solicitacao resultado = service.criarSolicitacao(novaSolicitacao(false, "descricao", "Alta"), usuarioCli);

        verify(usuarioRepository, times(1)).save(usuarioCli);
        assertSame(usuarioCli, resultado.getCidadao());
    }

    @Test
    @DisplayName("criarSolicitacao: nao anonimo nao valida tamanho minimo da descricao (comportamento atual)")
    void criarSolicitacao_naoAnonimoComDescricaoCurtaOuNull_salvaSemValidar() {
        Solicitacao comNull = novaSolicitacao(false, null, "Alta");
        Solicitacao curta = novaSolicitacao(false, "x", "Alta");

        assertDoesNotThrow(() -> service.criarSolicitacao(comNull, usuarioCli));
        assertDoesNotThrow(() -> service.criarSolicitacao(curta, usuarioCli));

        verify(repository, times(2)).save(any(Solicitacao.class));
    }

    // ---------- criarSolicitacao: campos gerados ----------

    @ParameterizedTest
    @CsvSource({
            "Alta,5 dias",
            "Média,15 dias",
            "Media,15 dias",
            "Baixa,30 dias",
            "'',30 dias",
            "Qualquer,30 dias",
            "Alta e Média,5 dias"
    })
    @DisplayName("criarSolicitacao: prioridade define o SLA")
    void criarSolicitacao_prioridade_defineSla(String prioridade, String slaEsperado) {
        Solicitacao resultado = service.criarSolicitacao(novaSolicitacao(false, "descricao", prioridade), usuarioCli);

        assertEquals(slaEsperado, resultado.getSla());
    }

    @Test
    @DisplayName("criarSolicitacao: prioridade null lanca NullPointerException e nao salva (comportamento atual, sem validacao explicita)")
    void criarSolicitacao_prioridadeNull_lancaNullPointerException() {
        Solicitacao s = novaSolicitacao(false, "descricao", null);

        assertThrows(NullPointerException.class, () -> service.criarSolicitacao(s, usuarioCli));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("criarSolicitacao: solicitacao null lanca NullPointerException e nao salva")
    void criarSolicitacao_solicitacaoNull_lancaNullPointerException() {
        assertThrows(NullPointerException.class, () -> service.criarSolicitacao(null, usuarioCli));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("criarSolicitacao: sempre sobrescreve o status para Aberto")
    void criarSolicitacao_qualquerCenarioValido_setaStatusAberto() {
        Solicitacao s = novaSolicitacao(false, "descricao", "Alta");
        s.setStatus("QualquerOutro");

        Solicitacao resultado = service.criarSolicitacao(s, usuarioCli);

        assertEquals("Aberto", resultado.getStatus());
    }

    @Test
    @DisplayName("criarSolicitacao: gera codigo REQ-ano-XXXX (hex maiusculo) e data dd/MM/yyyy")
    void criarSolicitacao_qualquerCenarioValido_geraCodigoEDataCorretos() {
        Solicitacao resultado = service.criarSolicitacao(novaSolicitacao(false, "descricao", "Alta"), usuarioCli);

        String regexCodigo = "^REQ-" + LocalDate.now().getYear() + "-[0-9A-F]{4}$";
        assertTrue(resultado.getCodigo().matches(regexCodigo),
                "Codigo gerado nao corresponde ao formato esperado: " + resultado.getCodigo());
        assertEquals(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), resultado.getData());
    }

    @Test
    @DisplayName("criarSolicitacao: retorna o objeto devolvido por repository.save, nao o de entrada")
    void criarSolicitacao_retornoEhObjetoDoRepositorySave() {
        Solicitacao entrada = novaSolicitacao(false, "descricao", "Alta");
        Solicitacao salvo = new Solicitacao();
        salvo.setCodigo("REQ-9999-ZZZZ");
        when(repository.save(any(Solicitacao.class))).thenReturn(salvo);

        Solicitacao resultado = service.criarSolicitacao(entrada, usuarioCli);

        assertSame(salvo, resultado);
        verify(repository, times(1)).save(entrada);
    }

    // ---------- listarTodas ----------

    @Test
    @DisplayName("listarTodas: retorna a lista do repository")
    void listarTodas_comElementos_retornaLista() {
        List<Solicitacao> lista = List.of(new Solicitacao(), new Solicitacao());
        when(repository.findAll()).thenReturn(lista);

        List<Solicitacao> resultado = service.listarTodas();

        assertSame(lista, resultado);
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("listarTodas: repository vazio retorna lista vazia")
    void listarTodas_semElementos_retornaListaVazia() {
        when(repository.findAll()).thenReturn(Collections.emptyList());

        assertTrue(service.listarTodas().isEmpty());
    }

    // ---------- buscarPorCodigo ----------

    @Test
    @DisplayName("buscarPorCodigo: codigo existente retorna a solicitacao")
    void buscarPorCodigo_codigoExistente_retornaObjeto() {
        Solicitacao s = new Solicitacao();
        s.setCodigo("REQ-2026-AAAA");
        when(repository.findById("REQ-2026-AAAA")).thenReturn(Optional.of(s));

        assertSame(s, service.buscarPorCodigo("REQ-2026-AAAA"));
    }

    @Test
    @DisplayName("buscarPorCodigo: codigo inexistente lanca RuntimeException")
    void buscarPorCodigo_codigoInexistente_lancaRuntimeException() {
        when(repository.findById("INEXISTENTE")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.buscarPorCodigo("INEXISTENTE"));

        assertEquals("Protocolo nao encontrado", ex.getMessage());
    }

    // ---------- atualizarStatus ----------

    @Test
    @DisplayName("atualizarStatus: atualiza o status, adiciona entrada no historico e salva")
    void atualizarStatus_codigoExistente_atualizaStatusHistoricoESalva() {
        Solicitacao s = new Solicitacao();
        s.setCodigo("REQ-2026-BBBB");
        s.setStatus("Aberto");
        when(repository.findById("REQ-2026-BBBB")).thenReturn(Optional.of(s));

        Solicitacao resultado = service.atualizarStatus("REQ-2026-BBBB", "Concluido");

        assertEquals("Concluido", resultado.getStatus());
        assertEquals(1, resultado.getHistorico().size());

        Atualizacao att = resultado.getHistorico().get(0);
        assertEquals("Status alterado para: Concluido", att.getNota());
        assertEquals("Gestor Público", att.getAutor());
        assertTrue(att.getDataHora().matches("\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}"),
                "Formato de dataHora inesperado: " + att.getDataHora());
        verify(repository, times(1)).save(s);
    }

    @Test
    @DisplayName("atualizarStatus: preserva historico anterior e acrescenta a nova entrada no fim")
    void atualizarStatus_comHistoricoPrevio_acrescentaNoFim() {
        Solicitacao s = new Solicitacao();
        s.setHistorico(new ArrayList<>(List.of(new Atualizacao("01/01/2026 10:00", "Abertura", "Sistema"))));
        when(repository.findById("REQ-1")).thenReturn(Optional.of(s));

        Solicitacao resultado = service.atualizarStatus("REQ-1", "Em andamento");

        assertEquals(2, resultado.getHistorico().size());
        assertEquals("Abertura", resultado.getHistorico().get(0).getNota());
        assertEquals("Status alterado para: Em andamento", resultado.getHistorico().get(1).getNota());
    }

    @Test
    @DisplayName("atualizarStatus: codigo inexistente propaga RuntimeException e nao salva")
    void atualizarStatus_codigoInexistente_propagaExcecaoNaoChamaSave() {
        when(repository.findById("INEXISTENTE")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.atualizarStatus("INEXISTENTE", "Concluido"));

        assertEquals("Protocolo nao encontrado", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("atualizarStatus: novoStatus null eh aceito sem validacao (comportamento atual)")
    void atualizarStatus_novoStatusNull_aceitoSemValidacao() {
        Solicitacao s = new Solicitacao();
        when(repository.findById("REQ-1")).thenReturn(Optional.of(s));

        Solicitacao resultado = service.atualizarStatus("REQ-1", null);

        assertNull(resultado.getStatus());
        verify(repository, times(1)).save(s);
    }

    // ---------- excluirSolicitacao ----------

    @Test
    @DisplayName("excluirSolicitacao: codigo existente chama delete com o objeto encontrado")
    void excluirSolicitacao_codigoExistente_chamaDelete() {
        Solicitacao s = new Solicitacao();
        when(repository.findById("REQ-2026-DDDD")).thenReturn(Optional.of(s));

        service.excluirSolicitacao("REQ-2026-DDDD");

        verify(repository, times(1)).delete(s);
    }

    @Test
    @DisplayName("excluirSolicitacao: codigo inexistente propaga RuntimeException e nao chama delete")
    void excluirSolicitacao_codigoInexistente_propagaExcecaoNaoChamaDelete() {
        when(repository.findById("INEXISTENTE")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.excluirSolicitacao("INEXISTENTE"));

        assertEquals("Protocolo nao encontrado", ex.getMessage());
        verify(repository, never()).delete(any());
    }
}