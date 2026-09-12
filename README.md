# AEP 2026 - SEGUNDO SEMESTRE
> ObservaAção

## Definição do Problema
O ObservaAção é um sistema de zeladoria urbana focado no cidadão. 
O objetivo é solucionar a dificuldade de comunicação entre a população e os órgãos públicos, facilitando o registro de demandas da cidade (como buracos na via, falta de iluminação ou problemas na limpeza urbana). 
A aplicação busca dar transparência a esse processo de atendimento. 
O projeto foi desenhado com base no **ODS 16** (Paz, Justiça e Instituições Eficazes) da ONU, garantindo que as instituições públicas sejam mais responsivas, acessíveis e transparentes para a sociedade.

## Funcionalidades
Esta primeira versão da Prova de Conceito (PoC) funciona via linha de comando (CLI) e contempla as operações básicas de CRUD utilizando uma única coleção no banco de dados NoSQL:

- **Registrar Solicitação (Create):** Criação de chamados informando categoria, prioridade e descrição. O sistema gera automaticamente um protocolo único de acompanhamento e define o prazo alvo (SLA) para a resolução.
- **Listar Solicitações (Read):** Exibição geral de todos os chamados ativos no sistema para acompanhamento rápido.
- **Ver Detalhes da Solicitação (Read):** Consulta completa de todas as informações e dados de um chamado específico utilizando o seu código de protocolo.
- **Atualizar Status (Update):** Modificação do andamento de uma solicitação existente (ex: Triagem, Em Execução, Resolvido) através do seu identificador.
- **Excluir Solicitação (Delete):** Remoção permanente de chamados do banco de dados.

## Tecnologias utilizadas

- **Java 26**: linguagem utilizada no desenvolvimento da aplicação orientada a objetos.
- **Spring Boot 4.1.1**: estrutura principal para configuração e execução da aplicação.
- **Spring Data MongoDB**: integração entre a aplicação Java e o banco de dados NoSQL.
- **MongoDB 8.0**: armazenamento das solicitações em documentos na coleção `solicitacoes`.
- **Maven Wrapper**: gerenciamento das dependências e execução do projeto.
- **Docker e Docker Compose**: criação e inicialização padronizada do ambiente MongoDB.
- **Jakarta Bean Validation**: validação dos dados obrigatórios das solicitações.
- **Lombok**: suporte à geração de código durante a compilação.

## Banco de dados MongoDB

A aplicação utiliza o banco NoSQL MongoDB. Por padrão, a conexão é realizada em
`mongodb://localhost:27017/observacao`, e as solicitações são armazenadas na coleção
`solicitacoes`.

### Iniciar o banco de dados

Com o Docker Desktop em execução, abra um terminal na pasta do projeto e execute:

```powershell
docker compose up -d
```

Verifique se o contêiner está saudável:

```powershell
docker compose ps
docker exec observacao-mongodb mongosh --quiet --eval "db.adminCommand('ping')"
```

O comando de verificação deve retornar `{ ok: 1 }`.

### Configurar outra conexão

Para utilizar outro servidor MongoDB, defina a variável de ambiente `MONGODB_URI`
antes de iniciar a aplicação:

```powershell
$env:MONGODB_URI="mongodb://localhost:27017/observacao"
```

### Consultar as solicitações

Abra o terminal do MongoDB dentro do contêiner:

```powershell
docker exec -it observacao-mongodb mongosh
```

Em seguida, selecione o banco e consulte a coleção:

```javascript
use observacao
db.solicitacoes.find()
```

### Encerrar o banco de dados

```powershell
docker compose down
```

Os dados permanecem armazenados no volume `mongodb_data` e estarão disponíveis na
próxima inicialização.

## Executando os testes

Este projeto possui dois tipos de teste:

### Testes unitários

Cobrem `SolicitacaoService` e `TerminalMenu` (JUnit 5 + Mockito). Não dependem de nenhum
serviço externo e podem ser executados a qualquer momento:

```bash
# Maven
mvn test
```

### Teste de integração (Testcontainers)

O `ObservacaoApplicationTests` sobe o contexto completo do Spring Boot com um MongoDB real,
via [Testcontainers](https://testcontainers.com/). Ele **requer Docker instalado e em
execução** na máquina.

```bash
# Maven
mvn test -Dtest=ObservacaoApplicationTests
```
