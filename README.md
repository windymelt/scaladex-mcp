# Scaladex-mcp

Provides Scaladex information on MCP.

- LLM can know latest library information.
- LLM can find library.

## Usage

### Codex

```sh
codex mcp add scaladex scala-cli PATH_TO_MAIN_SCALA
```

### Cline / Roo Code

```json
{
  "mcpServers": {
    "scaladex": {
      "command": "scala-cli",
      "args": ["PATH_TO_MAIN_SCALA"]
    }
  }
}
```

## Development

```sh
npx @modelcontextprotocol/inspector scala-cli run main.scala
```
