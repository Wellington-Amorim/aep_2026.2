package br.com.observacao.service;

import br.com.observacao.model.Atualizacao;
import br.com.observacao.model.Solicitacao;
import br.com.observacao.model.Usuario;
import br.com.observacao.repository.SolicitacaoRepository;
import br.com.observacao.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class SolicitacaoService {

    private final SolicitacaoRepository repository;
    private final UsuarioRepository usuarioRepository;

    public SolicitacaoService(SolicitacaoRepository repository, UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
    }

    public Solicitacao criarSolicitacao(Solicitacao solicitacao, Usuario usuarioCli) {
        if (solicitacao.isAnonimo()) {
            if (solicitacao.getDescricao() == null || solicitacao.getDescricao().length() < 20) {
                throw new IllegalArgumentException("Denuncias anonimas exigem no minimo 20 caracteres");
            }

            Usuario anonimo = usuarioRepository.findByEmail("anonimo@sistema.com")
                    .orElseGet(() -> usuarioRepository.save(new Usuario("Anonimo", "anonimo@sistema.com")));
            solicitacao.setCidadao(anonimo);
        } else {

            Usuario cidadaoDb = usuarioRepository.findByEmail(usuarioCli.getEmail())
                    .orElseGet(() -> usuarioRepository.save(usuarioCli));
            solicitacao.setCidadao(cidadaoDb);
        }

        solicitacao.setCodigo("REQ-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        solicitacao.setStatus("Aberto");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        solicitacao.setData(LocalDate.now().format(formatter));

        if (solicitacao.getPrioridade().contains("Alta")) {
            solicitacao.setSla("5 dias");
        } else if (solicitacao.getPrioridade().contains("Média") || solicitacao.getPrioridade().contains("Media")) {
            solicitacao.setSla("15 dias");
        } else {
            solicitacao.setSla("30 dias");
        }

        return repository.save(solicitacao);
    }

    public List<Solicitacao> listarTodas() {
        return repository.findAll();
    }

    public Solicitacao buscarPorCodigo(String codigo) {
        return repository.findById(codigo)
                .orElseThrow(() -> new RuntimeException("Protocolo nao encontrado"));
    }

    public Solicitacao atualizarStatus(String codigo, String novoStatus) {
        Solicitacao solicitacao = buscarPorCodigo(codigo);
        solicitacao.setStatus(novoStatus);

        Atualizacao novaAtt = new Atualizacao(
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                "Status alterado para: " + novoStatus,
                "Gestor Público"
        );
        solicitacao.getHistorico().add(novaAtt);

        return repository.save(solicitacao);
    }

    public void excluirSolicitacao(String codigo) {
        Solicitacao solicitacao = buscarPorCodigo(codigo);
        repository.delete(solicitacao);
    }
}
