import torch, yaml
model = torch.jit.load('model-19-03-2001-resnet18.pt')
ops = torch.jit.export_opnames(model)
with open('model-19-03-2001-resnet18.yaml', 'w') as output:
    yaml.dump(ops, output)
