# AEP 2026 - SEGUNDO SEMESTRE
> ObservaAção

# AEP 2026 - SEGUNDO SEMESTRE
> ObservaAção

**Versão final da AEP:** [ObservaAção v1.1.0](https://github.com/Wellington-Amorim/aep_2026.2/releases/tag/v1.1.0)

## Definição do Problema
O ObservaAção é um sistema de zeladoria urbana focado no cidadão.
O objetivo é solucionar a dificuldade de comunicação entre a população e os órgãos públicos, facilitando o registro de demandas da cidade (como buracos na via, falta de iluminação ou problemas na limpeza urbana).
A aplicação busca dar transparência a esse processo de atendimento.
O projeto foi desenhado com base no **ODS 16** (Paz, Justiça e Instituições Eficazes) da ONU, garantindo que as instituições públicas sejam mais responsivas, acessíveis e transparentes para a sociedade.

## Evolução do Banco de Dados (2º Semestre)
Para atender aos requisitos finais da disciplina, a modelagem no MongoDB foi evoluída para suportar **objetos complexos, subdocumentos e múltiplas coleções com relacionamentos**:
- **Objetos Aninhados:** A coleção principal `solicitacoes` agora embute a classe `Endereco` (logradouro, bairro, ponto de referência), estruturando os dados de localização.
- **Listas de Subdocumentos:** Foi adicionada uma lista de objetos `Atualizacao` (`historico`) para registrar os eventos do chamado de forma aninhada ao documento principal.
- **Múltiplas Coleções e Relacionamentos:** Os dados dos munícipes foram extraídos para uma coleção independente chamada `usuarios`. O relacionamento entre a solicitação e o usuário é mantido via referência (ID).

## Funcionalidades
Esta Prova de Conceito (PoC) final funciona via linha de comando (CLI) e contempla as operações de CRUD evoluídas no banco de dados NoSQL:

- **Registrar Solicitação (Create):** Criação de chamados informando categoria, prioridade, descrição e endereço estruturado. O sistema atrela a solicitação ao perfil do cidadão, gera um protocolo único e define o SLA.
- **Listar Solicitações (Read):** Exibe todas as solicitações cadastradas para acompanhamento.
- **Ver Detalhes da Solicitação (Read):** Consulta completa de todas as informações, dados do usuário criador e o histórico de atualizações de um chamado específico.
- **Atualizar Status (Update):** Modificação do andamento de uma solicitação existente gerando novos registros no histórico aninhado.
- **Excluir Solicitação (Delete):** Remoção permanente de chamados do banco de dados.

## Tecnologias utilizadas

- **Java 26**: linguagem utilizada no desenvolvimento da aplicação orientada a objetos.
- **Spring Boot 4.1.1**: estrutura principal para configuração e execução da aplicação.
- **Spring Data MongoDB**: integração avançada entre a aplicação Java e o banco de dados NoSQL (suporte a `@Document` e referências).
- **MongoDB 8.0**: armazenamento das entidades nas coleções `solicitacoes` e `usuarios`.
- **Maven Wrapper**: gerenciamento das dependências e execução do projeto.
- **Docker e Docker Compose**: criação e inicialização padronizada do ambiente MongoDB.
- **Jakarta Bean Validation**: validação dos dados obrigatórios das solicitações.
- **Lombok**: suporte à geração de código durante a compilação.
- **JaCoCo**: plugin que gera o relatório de cobertura dos testes automatizados.

## Como executar a aplicação

### Pré-requisitos

- JDK 26 instalado e disponível no terminal. Confira com `java -version`.
- Docker com Docker Compose. No Windows, abra o Docker Desktop antes de executar os comandos.
- Git para clonar o repositório.

### Windows (PowerShell)

```powershell
git clone https://github.com/Wellington-Amorim/aep_2026.2.git
cd aep_2026.2
docker compose up -d --wait
.\mvnw.cmd clean package -DskipTests
java -jar .\target\observacao-0.0.1-SNAPSHOT.jar
```

### Linux e macOS

```bash
git clone https://github.com/Wellington-Amorim/aep_2026.2.git
cd aep_2026.2
docker compose up -d --wait
bash mvnw clean package -DskipTests
java -jar target/observacao-0.0.1-SNAPSHOT.jar
```

No menu, escolha **1** para registrar uma solicitação e guarde o protocolo exibido. Use **3** para atualizar o status e **5** para consultar os detalhes e o histórico. Para sair, escolha **0**.

## Banco de dados MongoDB

A aplicação utiliza o banco NoSQL MongoDB. Por padrão, a conexão é realizada em `mongodb://localhost:27017/observacao`, gerenciando as coleções `solicitacoes` e `usuarios`.

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

Se o MongoDB estiver em outro servidor, defina a variável `MONGODB_URI` antes de iniciar a aplicação. Substitua `servidor-mongodb` pelo endereço do seu servidor.

**Windows (PowerShell)**

```powershell
$env:MONGODB_URI="mongodb://servidor-mongodb:27017/observacao"
```

**Linux e macOS**

```bash
export MONGODB_URI="mongodb://servidor-mongodb:27017/observacao"
```
### Consultar as solicitações

Abra o terminal do MongoDB dentro do contêiner:

```powershell
docker exec -it observacao-mongodb mongosh
```

Em seguida, selecione o banco e consulte as coleções:

```javascript
use observacao
db.usuarios.find()
db.solicitacoes.find()
```

### Encerrar o banco de dados

```powershell
docker compose down
```

Os dados permanecem armazenados no volume `mongodb_data` e estarão disponíveis na próxima inicialização.

## Testes automatizados e cobertura

A suíte inclui testes do menu interativo, da camada de serviço e da inicialização do contexto Spring Boot com MongoDB. O teste de integração usa Testcontainers para iniciar seu próprio contêiner, então o Docker precisa estar em execução.

### Executar os testes

Na pasta do projeto, use o comando correspondente ao seu sistema.

**Windows (PowerShell)**

```powershell
.\mvnw.cmd clean test
```

**Linux e macOS**

```bash
bash mvnw clean test
```

### Consultar o relatório de cobertura

Após executar os testes, abra `target/site/jacoco/index.html` no navegador. A cobertura mínima exigida pela avaliação é de **70%**. Confira o resultado na linha **Total** do relatório JaCoCo.
