# Scaladex-mcp

Provides Scaladex information on MCP.

- LLM can know latest library information.
- LLM can find library.

## Usage

### Codex

```sh
codex mcp add scaladex cs launch dev.capslock::scaladex-mcp:0.0.1
```

### Cline / Roo Code

```json
{
  "mcpServers": {
    "scaladex": {
      "command": "cs",
      "args": ["launch", "dev.capslock::scaladex-mcp:0.0.1"]
    }
  }
}
```

## Development

```sh
npx @modelcontextprotocol/inspector sbt run
```
