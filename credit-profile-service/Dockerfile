FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build
WORKDIR /src
COPY src/CreditProfileService/CreditProfileService.csproj src/CreditProfileService/
RUN dotnet restore src/CreditProfileService/CreditProfileService.csproj
COPY src/CreditProfileService/ src/CreditProfileService/
RUN dotnet publish src/CreditProfileService/CreditProfileService.csproj \
    -c Release -o /out /p:UseAppHost=false

FROM mcr.microsoft.com/dotnet/aspnet:8.0 AS runtime
WORKDIR /app
COPY --from=build /out ./
EXPOSE 8084
ENV ASPNETCORE_URLS=http://+:8084
ENTRYPOINT ["dotnet", "CreditProfileService.dll"]
