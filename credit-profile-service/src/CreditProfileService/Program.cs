using Mota.CreditProfile.Configuration;
using Mota.CreditProfile.Kafka;
using Mota.CreditProfile.Repositories;
using Mota.CreditProfile.Services;

var builder = WebApplication.CreateBuilder(args);

builder.Services.Configure<MongoSettings>(builder.Configuration.GetSection("Mongo"));
builder.Services.Configure<KafkaSettings>(builder.Configuration.GetSection("Kafka"));

builder.Services.AddSingleton<ICreditProfileRepository, MongoCreditProfileRepository>();
builder.Services.AddSingleton<IBureauLookupService, DeterministicBureauLookupStub>();
builder.Services.AddScoped<ICreditProfileService, CreditProfileAggregationService>();

builder.Services.AddHostedService<IncomeVerifiedConsumer>();

builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

var app = builder.Build();

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.MapControllers();

app.Run();
