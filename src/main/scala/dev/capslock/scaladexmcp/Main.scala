package dev.capslock.scaladexmcp

import mcp.*

object Main extends App {
  val mcp = MCPBuilder
    .create()
    .handle(initialize): req =>
      InitializeResult(
        capabilities =
          ServerCapabilities(tools = Some(ServerCapabilities.Tools())),
        protocolVersion = req.protocolVersion,
        serverInfo = Implementation("scaladex-mcp", "0.0.1"),
      )
    .handle(tools.list): req =>
      ListToolsResult(
        Seq(
          Tool(
            name = "search_scala_library",
            description = Some(
              "Search for Scala libraries. If you provide a repository name, it will return matching libraries from Scaladex. You can retrieve information such as latest versions, all versions, repository details, and homepage URL.",
            ),
            inputSchema = Tool.InputSchema(
              Some(
                ujson.Obj(
                  "name" -> ujson.Obj("type" -> ujson.Str("string")),
                ),
              ),
              required = Some(Seq("name")),
            ),
          ),
        ),
      )
    .handle(tools.call): req =>
      req.name match
        case "search_scala_library" => {
          val name = req.arguments.get.value.get("name").arr.head.str
          val results = ScaladexSearch.searchLibrary(name)

          // Convert results to JSON using upickle
          import upickle.default._
          val json = write(results)

          CallToolResult(
            content = List(
              TextContent(json),
            ),
          )
        }
        case other =>
          Error(ErrorCode.InvalidParams, s"Tool not found: $other")
    .run(SyncTransport.default)
}
