package br.com.observacao.service;

import br.com.observacao.model.Solicitacao;
import br.com.observacao.repository.SolicitacaoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SolicitacaoServiceTest {

    @Mock
    private SolicitacaoRepository repository;

    @InjectMocks
    private SolicitacaoService service;

    private Solicitacao novaSolicitacao(boolean anonimo, String descricao, String prioridade) {
        Solicitacao s = new Solicitacao();
        s.setAnonimo(anonimo);
        s.setDescricao(descricao);
        s.setPrioridade(prioridade);
        s.setCategoria("Categoria X");
        s.setLocalizacao("Local X");
        return s;
    }

    @Test
    @DisplayName("criarSolicitacao: anonimo com descricao de 19 caracteres lanca IllegalArgumentException")
    void criarSolicitacao_anonimoComDescricao19Caracteres_lancaIllegalArgumentException() {
        String descricao19 = "a".repeat(19);
        Solicitacao s = novaSolicitacao(true, descricao19, "Alta");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.criarSolicitacao(s));

        assertEquals("Denuncias anonimas exigem no minimo 20 caracteres", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("criarSolicitacao: anonimo com descricao de 20 caracteres aceito")
    void criarSolicitacao_anonimoComDescricao20Caracteres_naoLancaExcecao() {
        String descricao20 = "a".repeat(20);
        Solicitacao s = novaSolicitacao(true, descricao20, "Alta");
        when(repository.save(any(Solicitacao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Solicitacao resultado = assertDoesNotThrow(() -> service.criarSolicitacao(s));

        assertNotNull(resultado);
        verify(repository, times(1)).save(any(Solicitacao.class));
    }

    @Test
    @DisplayName("criarSolicitacao: anonimo com descricao de 21+ caracteres aceito")
    void criarSolicitacao_anonimoComDescricao21Caracteres_naoLancaExcecao() {
        String descricao21 = "a".repeat(21);
        Solicitacao s = novaSolicitacao(true, descricao21, "Alta");
        when(repository.save(any(Solicitacao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> service.criarSolicitacao(s));

        verify(repository, times(1)).save(any(Solicitacao.class));
    }

    @Test
    @DisplayName("criarSolicitacao: anonimo com descricao null lanca IllegalArgumentException")
    void criarSolicitacao_anonimoComDescricaoNull_lancaIllegalArgumentException() {
        Solicitacao s = novaSolicitacao(true, null, "Alta");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.criarSolicitacao(s));

        assertEquals("Denuncias anonimas exigem no minimo 20 caracteres", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("criarSolicitacao: nao anonimo com descricao null nao valida e salva normalmente")
    void criarSolicitacao_naoAnonimoComDescricaoNull_naoValidaESalva() {
        Solicitacao s = novaSolicitacao(false, null, "Alta");
        when(repository.save(any(Solicitacao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Solicitacao resultado = assertDoesNotThrow(() -> service.criarSolicitacao(s));

        assertNotNull(resultado);
        assertNull(resultado.getDescricao());
        verify(repository, times(1)).save(any(Solicitacao.class));
    }

    @Test
    @DisplayName("criarSolicitacao: nao anonimo com descricao vazia nao valida e salva normalmente")
    void criarSolicitacao_naoAnonimoComDescricaoVazia_naoValidaESalva() {
        Solicitacao s = novaSolicitacao(false, "", "Alta");
        when(repository.save(any(Solicitacao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Solicitacao resultado = assertDoesNotThrow(() -> service.criarSolicitacao(s));

        assertEquals("", resultado.getDescricao());
        verify(repository, times(1)).save(any(Solicitacao.class));
    }

    @Test
    @DisplayName("criarSolicitacao: quando anonimo=true, sobrescreve nomeCidadao e emailCidadao")
    void criarSolicitacao_anonimoTrue_sobrescreveNomeEEmailCidadao() {
        Solicitacao s = novaSolicitacao(true, "a".repeat(20), "Alta");
        s.setNomeCidadao("Joao");
        s.setEmailCidadao("joao@email.com");
        when(repository.save(any(Solicitacao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Solicitacao resultado = service.criarSolicitacao(s);

        assertEquals("Anonimo", resultado.getNomeCidadao());
        assertEquals("anonimo@sistema.com", resultado.getEmailCidadao());
    }

    @Test
    @DisplayName("criarSolicitacao: quando anonimo=false, nao sobrescreve nomeCidadao e emailCidadao")
    void criarSolicitacao_anonimoFalse_naoSobrescreveNomeEEmail() {
        Solicitacao s = novaSolicitacao(false, "descricao qualquer", "Alta");
        s.setNomeCidadao("Joao");
        s.setEmailCidadao("joao@email.com");
        when(repository.save(any(Solicitacao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Solicitacao resultado = service.criarSolicitacao(s);

        assertEquals("Joao", resultado.getNomeCidadao());
        assertEquals("joao@email.com", resultado.getEmailCidadao());
    }

    @Test
    @DisplayName("criarSolicitacao: prioridade Alta seta SLA de 5 dias")
    void criarSolicitacao_prioridadeAlta_setaSlaCincoDias() {
        Solicitacao s = novaSolicitacao(false, "descricao", "Alta");
        when(repository.save(any(Solicitacao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Solicitacao resultado = service.criarSolicitacao(s);

        assertEquals("5 dias", resultado.getSla());
    }

    @Test
    @DisplayName("criarSolicitacao: prioridade Media (com acento) seta SLA de 15 dias")
    void criarSolicitacao_prioridadeMediaComAcento_setaSlaQuinzeDias() {
        Solicitacao s = novaSolicitacao(false, "descricao", "Média");
        when(repository.save(any(Solicitacao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Solicitacao resultado = service.criarSolicitacao(s);

        assertEquals("15 dias", resultado.getSla());
    }

    @Test
    @DisplayName("criarSolicitacao: prioridade Media (sem acento) seta SLA de 15 dias")
    void criarSolicitacao_prioridadeMediaSemAcento_setaSlaQuinzeDias() {
        Solicitacao s = novaSolicitacao(false, "descricao", "Media");
        when(repository.save(any(Solicitacao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Solicitacao resultado = service.criarSolicitacao(s);

        assertEquals("15 dias", resultado.getSla());
    }

    @Test
    @DisplayName("criarSolicitacao: prioridade nao mapeada seta SLA de 30 dias")
    void criarSolicitacao_prioridadeNaoMapeada_setaSlaTrintaDias() {
        Solicitacao s = novaSolicitacao(false, "descricao", "Baixa");
        when(repository.save(any(Solicitacao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Solicitacao resultado = service.criarSolicitacao(s);

        assertEquals("30 dias", resultado.getSla());
    }

    @Test
    @DisplayName("criarSolicitacao: prioridade vazia cai no else e seta SLA de 30 dias")
    void criarSolicitacao_prioridadeVazia_setaSlaTrintaDias() {
        Solicitacao s = novaSolicitacao(false, "descricao", "");
        when(repository.save(any(Solicitacao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Solicitacao resultado = service.criarSolicitacao(s);

        assertEquals("30 dias", resultado.getSla());
    }

    @Test
    @DisplayName("criarSolicitacao: prioridade contendo Alta e Media simultaneamente prioriza o branch Alta")
    void criarSolicitacao_prioridadeAltaEMediaSimultaneamente_priorizaAlta() {
        Solicitacao s = novaSolicitacao(false, "descricao", "Alta e Média");
        when(repository.save(any(Solicitacao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Solicitacao resultado = service.criarSolicitacao(s);

        assertEquals("5 dias", resultado.getSla());
    }

    @Test
    @DisplayName("criarSolicitacao: prioridade null lanca NullPointerException")
    void criarSolicitacao_prioridadeNull_lancaNullPointerException() {
        Solicitacao s = novaSolicitacao(false, "descricao", null);

        assertThrows(NullPointerException.class, () -> service.criarSolicitacao(s));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("criarSolicitacao: solicitacao null lanca NullPointerException")
    void criarSolicitacao_solicitacaoNull_lancaNullPointerException() {
        assertThrows(NullPointerException.class, () -> service.criarSolicitacao(null));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("criarSolicitacao: sempre seta status como Aberto")
    void criarSolicitacao_qualquerCenarioValido_setaStatusAberto() {
        Solicitacao s = novaSolicitacao(false, "descricao", "Alta");
        s.setStatus("QualquerOutro");
        when(repository.save(any(Solicitacao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Solicitacao resultado = service.criarSolicitacao(s);

        assertEquals("Aberto", resultado.getStatus());
    }

    @Test
    @DisplayName("criarSolicitacao: gera codigo no formato REQ-ano-XXXX e data no formato dd/MM/yyyy")
    void criarSolicitacao_qualquerCenarioValido_geraCodigoEDataCorretos() {
        Solicitacao s = novaSolicitacao(false, "descricao", "Alta");
        when(repository.save(any(Solicitacao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Solicitacao resultado = service.criarSolicitacao(s);

        int anoAtual = LocalDate.now().getYear();
        String regexCodigo = "^REQ-" + anoAtual + "-[0-9A-F]{4}$";
        assertTrue(resultado.getCodigo().matches(regexCodigo),
                "Codigo gerado nao corresponde ao formato esperado: " + resultado.getCodigo());

        String dataEsperada = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        assertEquals(dataEsperada, resultado.getData());
    }

    @Test
    @DisplayName("criarSolicitacao: retorno do metodo eh o objeto retornado por repository.save, nao o objeto de entrada")
    void criarSolicitacao_retornoEhObjetoDoRepositorySave_naoObjetoDeEntrada() {
        Solicitacao entrada = novaSolicitacao(false, "descricao", "Alta");
        Solicitacao objetoSalvoRetornado = new Solicitacao();
        objetoSalvoRetornado.setCodigo("REQ-9999-ZZZZ");
        when(repository.save(any(Solicitacao.class))).thenReturn(objetoSalvoRetornado);

        Solicitacao resultado = service.criarSolicitacao(entrada);

        assertSame(objetoSalvoRetornado, resultado);
        assertNotSame(entrada, resultado);
        verify(repository, times(1)).save(entrada);
    }

    @Test
    @DisplayName("listarTodas: repository com elementos retorna a lista correspondente")
    void listarTodas_comElementos_retornaLista() {
        Solicitacao s1 = new Solicitacao();
        Solicitacao s2 = new Solicitacao();
        List<Solicitacao> lista = List.of(s1, s2);
        when(repository.findAll()).thenReturn(lista);

        List<Solicitacao> resultado = service.listarTodas();

        assertEquals(2, resultado.size());
        assertSame(lista, resultado);
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("listarTodas: repository sem elementos retorna lista vazia")
    void listarTodas_semElementos_retornaListaVazia() {
        when(repository.findAll()).thenReturn(Collections.emptyList());

        List<Solicitacao> resultado = service.listarTodas();

        assertTrue(resultado.isEmpty());
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("buscarPorCodigo: codigo existente retorna o objeto correspondente")
    void buscarPorCodigo_codigoExistente_retornaObjeto() {
        Solicitacao s = new Solicitacao();
        s.setCodigo("REQ-2026-AAAA");
        when(repository.findById("REQ-2026-AAAA")).thenReturn(Optional.of(s));

        Solicitacao resultado = service.buscarPorCodigo("REQ-2026-AAAA");

        assertSame(s, resultado);
        verify(repository, times(1)).findById("REQ-2026-AAAA");
    }

    @Test
    @DisplayName("buscarPorCodigo: codigo inexistente lanca RuntimeException")
    void buscarPorCodigo_codigoInexistente_lancaRuntimeException() {
        when(repository.findById("INEXISTENTE")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.buscarPorCodigo("INEXISTENTE"));

        assertEquals("Protocolo nao encontrado", ex.getMessage());
        verify(repository, times(1)).findById("INEXISTENTE");
    }

    @Test
    @DisplayName("atualizarStatus: codigo existente com novoStatus valido atualiza e salva")
    void atualizarStatus_codigoExistenteNovoStatusValido_atualizaESalva() {
        Solicitacao s = new Solicitacao();
        s.setCodigo("REQ-2026-BBBB");
        s.setStatus("Aberto");
        when(repository.findById("REQ-2026-BBBB")).thenReturn(Optional.of(s));
        when(repository.save(any(Solicitacao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Solicitacao resultado = service.atualizarStatus("REQ-2026-BBBB", "Concluido");

        assertEquals("Concluido", resultado.getStatus());
        verify(repository, times(1)).findById("REQ-2026-BBBB");
        verify(repository, times(1)).save(s);
    }

    @Test
    @DisplayName("atualizarStatus: codigo inexistente propaga RuntimeException e nao chama save")
    void atualizarStatus_codigoInexistente_propagaExcecaoNaoChamaSave() {
        when(repository.findById("INEXISTENTE")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.atualizarStatus("INEXISTENTE", "Concluido"));

        assertEquals("Protocolo nao encontrado", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("atualizarStatus: novoStatus null eh aceito sem validacao")
    void atualizarStatus_novoStatusNull_aceitoSemValidacao() {
        Solicitacao s = new Solicitacao();
        s.setCodigo("REQ-2026-CCCC");
        s.setStatus("Aberto");
        when(repository.findById("REQ-2026-CCCC")).thenReturn(Optional.of(s));
        when(repository.save(any(Solicitacao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Solicitacao resultado = service.atualizarStatus("REQ-2026-CCCC", null);

        assertNull(resultado.getStatus());
        verify(repository, times(1)).save(s);
    }

    @Test
    @DisplayName("excluirPorCodigo: codigo existente chama delete com o objeto correto")
    void excluirPorCodigo_codigoExistente_chamaDeleteComObjetoCorreto() {
        Solicitacao s = new Solicitacao();
        s.setCodigo("REQ-2026-DDDD");
        when(repository.findById("REQ-2026-DDDD")).thenReturn(Optional.of(s));

        service.excluirPorCodigo("REQ-2026-DDDD");

        verify(repository, times(1)).findById("REQ-2026-DDDD");
        verify(repository, times(1)).delete(s);
    }

    @Test
    @DisplayName("excluirPorCodigo: codigo inexistente propaga RuntimeException e nao chama delete")
    void excluirPorCodigo_codigoInexistente_propagaExcecaoNaoChamaDelete() {
        when(repository.findById("INEXISTENTE")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.excluirPorCodigo("INEXISTENTE"));

        assertEquals("Protocolo nao encontrado", ex.getMessage());
        verify(repository, never()).delete(any());
    }
}