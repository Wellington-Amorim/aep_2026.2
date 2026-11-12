package br.com.observacao.model;

import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "solicitacoes")
public class Solicitacao {

    @Id
    private String codigo;

    @NotBlank
    private String categoria;

    @NotBlank
    private String prioridade;

    @NotBlank
    private String descricao;

    @DBRef
    private Usuario cidadao;

    private String status;
    private Endereco endereco;
    private List<Atualizacao> historico = new ArrayList<>();
    private String data;
    private String sla;
    private boolean anonimo;

    public Solicitacao() { }

    public String getCodigo() {
        return codigo;
    }
    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getCategoria() {
        return categoria;
    }
    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getPrioridade() {
        return prioridade;
    }
    public void setPrioridade(String prioridade) {
        this.prioridade = prioridade;
    }

    public String getDescricao() {
        return descricao;
    }
    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public Endereco getEndereco() { return endereco; }
    public void setEndereco(Endereco endereco) { this.endereco = endereco; }

    public List<Atualizacao> getHistorico() { return historico; }
    public void setHistorico(List<Atualizacao> historico) { this.historico = historico; }

    public String getData() {
        return data;
    }
    public void setData(String data) {
        this.data = data;
    }

    public String getSla() {
        return sla;
    }
    public void setSla(String sla) {
        this.sla = sla;
    }

    public boolean isAnonimo() {
        return anonimo;
    }
    public void setAnonimo(boolean anonimo) {
        this.anonimo = anonimo;
    }

    public Usuario getCidadao() { return cidadao; }
    public void setCidadao(Usuario cidadao) { this.cidadao = cidadao; }
}
