package br.com.observacao.controller;

import jakarta.validation.Valid;
import br.com.observacao.model.Solicitacao;
import br.com.observacao.service.SolicitacaoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/solicitacoes")
public class SolicitacaoController {

    private final SolicitacaoService service;

    public SolicitacaoController(SolicitacaoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Solicitacao> criar(@Valid @RequestBody Solicitacao solicitacao) {
        return new ResponseEntity<>(service.criarSolicitacao(solicitacao), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Solicitacao>> listar() {
        return ResponseEntity.ok(service.listarTodas());
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<Solicitacao> buscar(@PathVariable String codigo) {
        return ResponseEntity.ok(service.buscarPorCodigo(codigo));
    }

    @PatchMapping("/{codigo}/status")
    public ResponseEntity<Solicitacao> atualizarStatus(@PathVariable String codigo, @RequestBody String novoStatus) {
        return ResponseEntity.ok(service.atualizarStatus(codigo, novoStatus));
    }
}