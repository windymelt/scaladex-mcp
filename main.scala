//> using scala 3.7.0
//> using dep com.indoorvivants::mcp-quick::latest.release
//> using dep com.softwaremill.sttp.client4::core::4.0.13

import mcp.*

@main def run =
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
            description = Some("Search for Scala libraries"),
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
end run

object ScaladexSearch:
  import sttp.client4.quick.*
  import upickle.default.{ReadWriter => RW, macroRW}

  object Api:
    val baseUrl = uri"https://index.scala-lang.org/api/v1"
    case class Project(
        organization: String,
        repository: String,
    )
    object Project:
      implicit val rw: RW[Project] = macroRW

    /*
only organization and repository, topics, documentationLinks, contributerWanted, cliArtifacts are required. others can be undefined.
     */
    case class Repository(
        organization: String,
        repository: String,
        homepage: Option[String] = None,
        description: Option[String] = None,
        logo: Option[String] = None,
        stars: Option[Int] = None,
        forks: Option[Int] = None,
        issues: Option[Int] = None,
        topics: Seq[String],
        contributingGuide: Option[String] = None,
        codeOfConduct: Option[String] = None,
        license: Option[String] = None,
        defaultArtifact: Option[String] = None,
        customScalaDoc: Option[String] = None,
        documentationLinks: Seq[DocumentationLink],
        contributorsWanted: Boolean,
        cliArtifacts: Seq[String],
        category: Option[String] = None,
        chatroom: Option[String] = None,
    )
    object Repository:
      implicit val rw: RW[Repository] = macroRW

    case class DocumentationLink(
        label: String,
        pattern: String,
    )
    object DocumentationLink:
      implicit val rw: RW[DocumentationLink] = macroRW

    case class RepositoryVersion(
        groupId: String,
        artifactId: String,
        version: String,
    )
    object RepositoryVersion:
      implicit val rw: RW[RepositoryVersion] = macroRW

    case class Artifact(
        groupId: String,
        artifactId: String,
        version: String,
        name: String,
        binaryVersion: String,
        language: String,
        platform: String,
        project: Project,
        releaseDate: String,
        licenses: Seq[String],
    )
    object Artifact:
      implicit val rw: RW[Artifact] = macroRW

    val jsonRequest =
      basicRequest
        .header("Accept", "application/json")

    def projects(
        language: Option[Seq[String]] = Some(
          List("3", "2.13", "2.12", "2.11", "java"),
        ),
        platform: Option[Seq[String]] = Some(
          List("jvm", "sjs1", "native0.5", "sbt1", "mill0.11"),
        ),
    ): sttp.client4.Request[Seq[Project]] =
      // specify multiple params as repeat of key values
      val url = baseUrl
        .addPath("projects")
        .withParams(
          Seq(
            language
              .map(langs => langs.flatMap(lang => Seq("language" -> lang)))
              .getOrElse(Seq.empty),
            platform
              .map(plats => plats.flatMap(plat => Seq("platform" -> plat)))
              .getOrElse(Seq.empty),
          ).flatten.toMap,
        )
      System.err.println(s"Fetching projects from URL: $url")
      jsonRequest
        .get(
          url,
        )
        .mapResponse: resp =>
          resp match
            case Right(value) =>
              upickle.default.read[Seq[Project]](value)
            case Left(error) =>
              Seq.empty

    def repository(
        organization: String,
        repository: String,
    ): sttp.client4.Request[Repository] =
      jsonRequest
        .get(
          baseUrl.addPath(
            "projects",
            organization,
            repository,
          ),
        )
        .mapResponse: resp =>
          resp match
            case Right(value) =>
              upickle.default.read[Repository](value)
            case Left(error) =>
              throw new Exception(s"Failed to get repository: $error")

    def repositoryVersions(
        organization: String,
        repository: String,
    ): sttp.client4.Request[Seq[String]] =
      jsonRequest
        .get(
          baseUrl.addPath(
            "projects",
            organization,
            repository,
            "versions",
          ),
        )
        .mapResponse: resp =>
          resp match
            case Right(value) =>
              upickle.default.read[Seq[String]](value)
            case Left(error) =>
              Seq.empty

    def repositoryLatestVersion(
        organization: String,
        repository: String,
    ): sttp.client4.Request[List[RepositoryVersion]] =
      jsonRequest
        .get(
          baseUrl.addPath(
            "projects",
            organization,
            repository,
            "versions",
            "latest",
          ),
        )
        .mapResponse: resp =>
          resp match
            case Right(value) =>
              upickle.default.read[List[RepositoryVersion]](value)
            case Left(error) =>
              throw new Exception(s"Failed to get latest version: $error")

    def repositoryVersionInfo(
        organization: String,
        repository: String,
        version: String,
    ): sttp.client4.Request[List[RepositoryVersion]] =
      jsonRequest
        .get(
          baseUrl.addPath(
            "projects",
            organization,
            repository,
            "versions",
            version,
          ),
        )
        .mapResponse: resp =>
          resp match
            case Right(value) =>
              upickle.default.read[List[RepositoryVersion]](value)
            case Left(error) =>
              throw new Exception(s"Failed to get version info: $error")

    def repositoryArtifacts(
        organization: String,
        repository: String,
        binaryVersion: Option[String] = None,
        artifactName: Option[String] = None,
        stableOnly: Option[Boolean] = None,
    ): sttp.client4.Request[List[RepositoryVersion]] =
      jsonRequest
        .get(
          baseUrl
            .addPath(
              "projects",
              organization,
              repository,
              "artifacts",
            )
            .addParams(
              Seq(
                binaryVersion.map(v => "binary-version" -> v),
                artifactName.map(v => "artifact-name" -> v),
                stableOnly.map(v => "stable-only" -> v.toString),
              ).flatten.toMap,
            ),
        )
        .mapResponse: resp =>
          resp match
            case Right(value) =>
              upickle.default.read[List[RepositoryVersion]](value)
            case Left(error) =>
              List.empty

    def artifact(
        groupId: String,
        artifactId: String,
        stableOnly: Option[Boolean] = None,
    ): sttp.client4.Request[List[String]] =
      jsonRequest
        .get(
          baseUrl
            .addPath(
              "artifacts",
              groupId,
              artifactId,
            )
            .addParams(
              stableOnly
                .map(v => Map("stable-only" -> v.toString))
                .getOrElse(Map.empty),
            ),
        )
        .mapResponse: resp =>
          resp match
            case Right(value) =>
              upickle.default.read[List[String]](value)
            case Left(error) =>
              throw new Exception(s"Failed to get artifact: $error")

    def latestArtifact(
        groupId: String,
        artifactId: String,
    ): sttp.client4.Request[Artifact] =
      jsonRequest
        .get(
          baseUrl.addPath(
            "artifacts",
            groupId,
            artifactId,
            "latest",
          ),
        )
        .mapResponse: resp =>
          resp match
            case Right(value) =>
              upickle.default.read[Artifact](value)
            case Left(error) =>
              throw new Exception(
                s"Failed to get latest artifact version: $error",
              )

    def artifactVersionInfo(
        groupId: String,
        artifactId: String,
        version: String,
    ): sttp.client4.Request[Artifact] =
      jsonRequest
        .get(
          baseUrl.addPath(
            "artifacts",
            groupId,
            artifactId,
            version,
          ),
        )
        .mapResponse: resp =>
          resp match
            case Right(value) =>
              upickle.default.read[Artifact](value)
            case Left(error) =>
              throw new Exception(
                s"Failed to get artifact version info: $error",
              )

  end Api

  case class LibraryInfo(
      latestVersions: Seq[String],
      versions: Seq[String],
      repository: Api.Repository,
      url: Option[String] = None,
  )
  object LibraryInfo:
    implicit val rw: RW[LibraryInfo] = macroRW

  def searchLibrary(name: String): Seq[LibraryInfo] =
    // fetch all projects and filter by name.
    val allProjects = Api
      .projects(None, None)
      .send()
      .body
    System.err.println(s"Total projects fetched: ${allProjects.size}")
    allProjects
      .filter(_.repository.toLowerCase == name.toLowerCase)
      .flatMap { project =>
        val repository =
          Api
            .repository(project.organization, project.repository)
            .send()
            .body
        val vers = Api
          .repositoryVersions(
            project.organization,
            project.repository,
          )
          .send()
          .body
        System.err.println(
          s"Found repository: ${project.organization}/${project.repository} with ${vers.size} versions",
        )
        val latestVer = Api
          .repositoryLatestVersion(
            project.organization,
            project.repository,
          )
          .send()
          .body

        Seq(
          LibraryInfo(
            latestVersions =
              latestVer.map(v => s"${v.groupId}:${v.artifactId}:${v.version}"),
            versions = vers,
            repository = repository,
            url = repository.homepage,
          ),
        )
      }
end ScaladexSearch
